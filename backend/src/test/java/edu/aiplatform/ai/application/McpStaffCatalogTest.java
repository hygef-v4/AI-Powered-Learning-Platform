package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.ai.domain.McpCredential.Access;
import edu.aiplatform.identity.contract.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class McpStaffCatalogTest {
    final ObjectMapper json=new ObjectMapper();
    List<IMcpToolProvider> catalog() {
        return List.of(new McpCapabilitiesTools(),new McpAuthoringRequirementsTools(null,json),
                new edu.aiplatform.academics.application.AcademicsMcpTools(null,null),
                new edu.aiplatform.assignments.application.AssignmentMcpTools(null,null,null,null,null,null,json),
                new edu.aiplatform.assignments.application.AssignmentMcpQueryTools(null),
                new edu.aiplatform.assignments.application.AssignmentMcpAuthoringService(null,null,null,null,json),
                new edu.aiplatform.assignmentwork.application.AssignmentWorkMcpTools(null,null,null,json),
                new edu.aiplatform.quizzes.application.QuizMcpTools(null,json),
                new edu.aiplatform.quizzes.application.QuizMcpQueryTools(null),
                new edu.aiplatform.quizzes.application.QuizMcpAuthoringService(null,null,null,null,json),
                new edu.aiplatform.quizzes.application.QuestionBankMcpTools(null,null,null,null,null,json),
                new edu.aiplatform.materials.application.MaterialMcpTools(null,null),
                new edu.aiplatform.materials.application.MaterialMcpQueryTools(null),
                new edu.aiplatform.grading.application.GradingMcpTools(null,null,json),
                new edu.aiplatform.grading.application.GradingMcpQueryService(null,null,null,null,json),
                new edu.aiplatform.grading.application.GradingMcpBatchService(null,new edu.aiplatform.grading.application.GradingMcpTools(null,null,json),json),
                new AdminAiMcpTools(null,null),new edu.aiplatform.admindashboard.application.AdminDashboardMcpTools(null));
    }
    @Test void allSchemasCompileAndEveryRoleTokenCombinationHasCorrectCatalog() {
        var providers=catalog();var checker=new JsonSchemaChecker(json);
        var definitions=providers.stream().flatMap(p -> p.tools().stream()).toList();
        assertThat(definitions).allMatch(d -> checker.validSchema(d.parameterSchema()));
        assertThat(definitions).extracting(McpToolDefinition::name).doesNotHaveDuplicates()
                .contains("read_own_work","record_private_feedback");
        ObjectProvider<IMcpToolProvider> source=mock(ObjectProvider.class);
        when(source.orderedStream()).thenAnswer(call -> providers.stream());
        var registry=new McpToolRegistry(source);
        for(var access:Access.values()) {
            var learner=registry.visible(Role.STUDENT,access);
            assertThat(learner).extracting(McpToolDefinition::name)
                    .contains("get_my_capabilities","read_assignment_context","read_own_work")
                    .doesNotContain("read_ai_usage_summary","read_platform_health_summary","read_grading_context",
                            "create_assignment_draft","save_assignment_draft","record_grading_proposal","read_quiz_context");
            assertThat(learner.stream().anyMatch(d -> "record_private_feedback".equals(d.name())))
                    .isEqualTo(access==Access.READ_WRITE);
            if(access==Access.READ_ONLY)assertThat(learner).allMatch(d -> d.kind()==McpToolKind.READ);
            assertThat(registry.visible(Role.ADMIN,access)).isEmpty();
            for(var role:List.of(Role.TEACHER,Role.SUBJECT_MANAGER)) {
                var tools=registry.visible(role,access);
                assertThat(tools).extracting(McpToolDefinition::name).contains("get_my_capabilities","read_grading_context","list_my_classes")
                        .doesNotContain("read_ai_usage_summary","read_platform_health_summary");
                if(access==Access.READ_ONLY)assertThat(tools).allMatch(d -> d.kind()==McpToolKind.READ);
                else assertThat(tools).extracting(McpToolDefinition::name).contains("create_assignment_draft","create_quiz_draft","record_grading_proposals_batch");
                assertThat(tools.stream().anyMatch(d -> "save_question_bank_draft".equals(d.name())))
                        .isEqualTo(role==Role.SUBJECT_MANAGER&&access==Access.READ_WRITE);
            }
        }
    }

    @Test void everyVisibleToolPassesProtocolValidationForEveryRoleAndTokenKind() throws Exception {
        var definitions=catalog().stream().flatMap(p -> p.tools().stream()).toList();
        IMcpToolProvider fixture=new IMcpToolProvider() {
            public List<McpToolDefinition> tools(){return definitions;}
            public McpToolResult call(Actor actor,String name,Map<String,Object> args){return new McpToolResult(false,Map.of("fixture",true));}
        };
        ObjectProvider<IMcpToolProvider> source=mock(ObjectProvider.class);when(source.orderedStream()).thenAnswer(call -> java.util.stream.Stream.of(fixture));
        var registry=new McpToolRegistry(source);var limiter=mock(IMcpRateLimiter.class);when(limiter.tryAcquire(any())).thenReturn(true);
        var usage=mock(AiUsageService.class);var audit=mock(edu.aiplatform.audit.contract.IAudit.class);
        var server=new McpServer(registry,limiter,new JsonSchemaChecker(json),usage,audit,json,java.time.Clock.systemUTC());
        for(var role:Role.values())for(var access:Access.values()) {
            var actor=new Actor(UUID.randomUUID(),role);var now=java.time.Instant.now();
            var principal=new McpCredentialService.McpPrincipal(new edu.aiplatform.ai.domain.McpCredential(UUID.randomUUID(),actor.accountId(),
                    "fixture",access,"hash","test",now.plusSeconds(60),null,null,now),actor);
            var initialized=server.handle(principal,json.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\"}}" )).orElseThrow();
            assertThat(initialized.at("/result/serverInfo/name").asText()).isEqualTo("ai-learning-platform");
            var listed=server.handle(principal,json.readTree("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}")).orElseThrow();
            assertThat(listed.at("/result/tools").size()).isEqualTo(registry.visible(role,access).size());
            for(var tool:registry.visible(role,access)) {
                ObjectNode request=json.createObjectNode();request.put("jsonrpc","2.0").put("id",3).put("method","tools/call");
                request.putObject("params").put("name",tool.name()).set("arguments",sample(json.readTree(tool.parameterSchema())));
                var response=server.handle(principal,request).orElseThrow();
                assertThat(response.has("error")).as(role+" "+access+" "+tool.name()+" "+response).isFalse();
                assertThat(response.at("/result/isError").asBoolean()).as(tool.name()).isFalse();
            }
        }
    }
    private JsonNode sample(JsonNode schema) {
        if(schema.has("const"))return schema.get("const");
        if(schema.has("enum"))return schema.get("enum").get(0);
        if(schema.has("oneOf"))return sample(schema.get("oneOf").get(0));
        if(schema.has("anyOf"))return sample(schema.get("anyOf").get(0));
        var type=schema.path("type");String name=type.isArray()?type.get(0).asText():type.asText("object");
        return switch(name) {
            case "string" -> TextNode.valueOf("uuid".equals(schema.path("format").asText())?"12345678-1234-1234-1234-123456789abc":"fixture");
            case "integer","number" -> IntNode.valueOf(schema.has("minimum")?schema.path("minimum").asInt():1);
            case "boolean" -> BooleanNode.FALSE;
            case "null" -> NullNode.instance;
            case "array" -> {var array=json.createArrayNode();int count=Math.max(1,schema.path("minItems").asInt(1));for(int n=0;n<count;n++)array.add(sample(schema.path("items")));yield array;}
            default -> {var object=json.createObjectNode();schema.path("required").forEach(key -> object.set(key.asText(),sample(schema.path("properties").path(key.asText()))));yield object;}
        };
    }
}
