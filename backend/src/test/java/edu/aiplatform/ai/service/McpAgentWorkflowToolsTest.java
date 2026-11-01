package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.ai.model.McpCredential;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;

class McpAgentWorkflowToolsTest {
    final ObjectMapper json=new ObjectMapper();
    final McpAgentWorkflowTools provider=new McpAgentWorkflowTools(json);
    final Actor teacher=new Actor(UUID.randomUUID(),Role.TEACHER);

    @ParameterizedTest @ValueSource(strings={"AUTHOR_ASSIGNMENT","GRADE_WITH_RUBRIC"})
    void servesCompleteCanonicalInstructionsWithContentVersion(String kind) throws Exception {
        var result=provider.call(teacher,"read_agent_workflow",Map.of("workflow",kind));
        var content=(Map<String,Object>)result.content();
        String instructions=(String)content.get("instructions");
        String directory=kind.equals("AUTHOR_ASSIGNMENT")?"author-assignment":"grade-with-rubric";
        String canonical=Files.readString(Path.of("src/main/resources/agent-workflows",directory,"SKILL.md"))
                .replace("\r\n","\n");
        assertThat(result.error()).isFalse();
        assertThat(instructions).isEqualTo(canonical).contains("get_my_capabilities","reviewUrl");
        assertThat(instructions).contains(kind.equals("AUTHOR_ASSIGNMENT")?"grouped clarification":"snapshotFingerprint");
        assertThat(content.get("version")).isEqualTo(McpFingerprint.of(json,instructions))
                .isNotEqualTo(McpFingerprint.of(json,instructions+"\nChanged policy"));
        assertThat(provider.call(teacher,"read_agent_workflow",Map.of("workflow",kind))).isEqualTo(result);
        assertThat(instructions.getBytes(StandardCharsets.UTF_8).length).isLessThan(32*1024);
    }

    @ParameterizedTest @ValueSource(strings={"", "QUIZ", "../../private", "author-assignment"})
    void rejectsUnknownWorkflowWithoutResolvingAUserControlledPath(String kind) {
        assertThatThrownBy(() -> provider.call(teacher,"read_agent_workflow",Map.of("workflow",kind)))
                .isInstanceOf(ApiException.class);
    }

    @Test void onlyTeachingRolesCanReadAndDescriptionsPointToTheServerWorkflow() {
        for(var role:List.of(Role.STUDENT,Role.ADMIN))
            assertThatThrownBy(() -> provider.call(new Actor(UUID.randomUUID(),role),"read_agent_workflow",
                    Map.of("workflow","AUTHOR_ASSIGNMENT"))).isInstanceOf(ApiException.class);
        assertThat(provider.call(new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER),"read_agent_workflow",
                Map.of("workflow","GRADE_WITH_RUBRIC")).error()).isFalse();
        assertThatThrownBy(() -> provider.call(teacher,"unknown_tool",Map.of("workflow","AUTHOR_ASSIGNMENT")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> provider.call(teacher,"read_agent_workflow",Map.of())).isInstanceOf(ApiException.class);
        assertThat(McpCapabilitiesTools.workflow(Role.TEACHER)).contains("read_agent_workflow","AUTHOR_ASSIGNMENT","GRADE_WITH_RUBRIC");
        assertThat(McpCapabilitiesTools.workflow(Role.STUDENT)).doesNotContain("AUTHOR_ASSIGNMENT","GRADE_WITH_RUBRIC");
    }

    @Test void realProtocolCanReadWorkflowWithAReadOnlyCredentialAndNoLocalSkill() throws Exception {
        ObjectProvider<IMcpToolProvider> source=mock(ObjectProvider.class);
        when(source.orderedStream()).thenAnswer(call -> java.util.stream.Stream.of(provider,new McpCapabilitiesTools()));
        var limiter=mock(IMcpRateLimiter.class);when(limiter.tryAcquire(any())).thenReturn(true);
        var server=new McpServer(new McpToolRegistry(source),limiter,new JsonSchemaChecker(json),
                mock(AiUsageService.class),mock(edu.aiplatform.audit.contract.IAudit.class),json,Clock.systemUTC());
        var now=Instant.now();var credential=new McpCredential(UUID.randomUUID(),teacher.accountId(),"fixture",
                McpCredential.Access.READ_ONLY,"hash","test",now.plusSeconds(60),null,null,now);
        var principal=new McpCredentialService.McpPrincipal(credential,teacher);
        var response=server.handle(principal,json.readTree("""
                {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{
                  "name":"read_agent_workflow","arguments":{"workflow":"GRADE_WITH_RUBRIC"}}}
                """)).orElseThrow();
        assertThat(response.at("/result/isError").asBoolean()).isFalse();
        var content=json.readTree(response.at("/result/content/0/text").asText());
        assertThat(content.path("instructions").asText()).contains("snapshotFingerprint","requestRef","reviewUrl");
        assertThat(content.path("version").asText()).matches("[0-9a-f]{64}");
        assertThat(provider.tools().get(0).kind()).isEqualTo(McpToolKind.READ);
    }

    @ParameterizedTest @ValueSource(strings={"ASSIGNMENT","QUIZ","BANK"})
    void authoringRequirementsRouteOnlyAssignmentsToTheAssignmentWorkflow(String kind) {
        var registry=mock(McpToolRegistry.class);
        var definition=new McpToolDefinition("fixture","fixture","{\"type\":\"object\"}",McpToolKind.WRITE,McpSchemas.STAFF);
        when(registry.find(anyString(),eq(Role.TEACHER),eq(McpCredential.Access.READ_WRITE)))
                .thenReturn(Optional.of(new McpToolRegistry.Entry(definition,provider)));
        var requirements=new McpAuthoringRequirementsTools(registry,json);
        var content=(Map<String,Object>)requirements.call(teacher,"read_authoring_requirements",Map.of("kind",kind)).content();
        String workflow=(String)content.get("workflow");
        if("ASSIGNMENT".equals(kind))assertThat(workflow).contains("read_agent_workflow","AUTHOR_ASSIGNMENT");
        else assertThat(workflow).doesNotContain("workflow=AUTHOR_ASSIGNMENT").contains("QUIZ/BANK","clarification");
    }

    @ParameterizedTest @ValueSource(strings={"AUTHOR_ASSIGNMENT","GRADE_WITH_RUBRIC"})
    void optionalLocalSkillIsOnlyAServerWorkflowWrapper(String kind) throws Exception {
        String directory=kind.equals("AUTHOR_ASSIGNMENT")?"author-assignment":"grade-with-rubric";
        String wrapper=Files.readString(Path.of("..","agent-skills",directory,"SKILL.md"));
        assertThat(wrapper).contains("read_agent_workflow",kind,"optional","Do not fall back");
        assertThat(wrapper).doesNotContain("create_assignment_draft","record_grading_proposal");
    }
}
