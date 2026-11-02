package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.service.McpCredentialService.McpPrincipal;
import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.ai.repository.AiUsageRepository;
import edu.aiplatform.ai.model.McpCredential;
import edu.aiplatform.ai.model.McpCredential.Access;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class McpServerTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    private static final String SCHEMA = """
            {"type":"object","properties":{"q":{"type":"string"}},"required":["q"],"additionalProperties":false}""";

    private final ObjectMapper json = new ObjectMapper();
    private final IMcpToolProvider provider = mock(IMcpToolProvider.class);
    private final IMcpRateLimiter limiter = mock(IMcpRateLimiter.class);
    private final AiUsageRepository usageRepository = mock(AiUsageRepository.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private McpServer server;
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(provider.tools()).thenReturn(List.of(
                new McpToolDefinition("read_own_work", "Đọc bài", SCHEMA, McpToolKind.READ, Set.of(Role.TEACHER)),
                new McpToolDefinition("save_draft", "Lưu nháp", SCHEMA, McpToolKind.WRITE, Set.of(Role.TEACHER)),
                new McpToolDefinition("teacher_only", "GV", SCHEMA, McpToolKind.READ, Set.of(Role.ADMIN))));
        ObjectProvider<IMcpToolProvider> providers = mock(ObjectProvider.class);
        when(providers.orderedStream()).thenAnswer(i -> Stream.of(provider));
        AiUsageService usage = new AiUsageService(usageRepository,
                new AiAccess(mock(IAuthorization.class), audit, clock), clock);
        server = new McpServer(new McpToolRegistry(providers), limiter, new JsonSchemaChecker(json), usage, audit, json,
                clock);
        when(limiter.tryAcquire(any())).thenReturn(true);
    }

    @Test
    void initializeNegotiatesProtocolAndNotificationsHaveNoReply() {
        JsonNode reply = handle(Access.READ_ONLY,
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\"}}");
        assertThat(reply.at("/result/protocolVersion").asText()).isEqualTo("2025-03-26");
        assertThat(reply.at("/result/capabilities/tools").isObject()).isTrue();
        reply = handle(Access.READ_ONLY,
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"1999-01-01\"}}");
        assertThat(reply.at("/result/protocolVersion").asText()).isEqualTo(McpServer.LATEST_PROTOCOL);

        assertThat(server.handle(principal(Access.READ_ONLY),
                read("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))).isEmpty();
        assertThat(handle(Access.READ_ONLY, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"resources/list\"}")
                .at("/error/code").asInt()).isEqualTo(-32601);
        assertThat(handle(Access.READ_ONLY, "[{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"ping\"}]")
                .at("/error/code").asInt()).isEqualTo(-32600);
    }

    @Test
    void toolsListFiltersByRoleAndReadOnlyCredential() {
        JsonNode readOnly = handle(Access.READ_ONLY, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
        assertThat(readOnly.at("/result/tools").findValuesAsText("name")).containsExactly("read_own_work");
        assertThat(readOnly.at("/result/tools/0/inputSchema/type").asText()).isEqualTo("object");

        JsonNode readWrite = handle(Access.READ_WRITE, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
        assertThat(readWrite.at("/result/tools").findValuesAsText("name")).containsExactly("read_own_work", "save_draft");
    }

    @Test
    void writeToolIsHiddenFromReadOnlyCredential() {
        JsonNode reply = handle(Access.READ_ONLY, call("save_draft", "{\"q\":\"x\"}"));

        assertThat(reply.at("/error/code").asInt()).isEqualTo(-32602);
        verify(provider, never()).call(any(), anyString(), any());
        assertThat(actions()).contains("ACCESS_DENIED");
    }

    @Test
    void callRunsToolWithCredentialActorAndRecordsUsageAndAudit() {
        when(provider.call(eq(teacher), eq("read_own_work"), eq(Map.of("q", "bài 1"))))
                .thenReturn(new McpToolResult(false, Map.of("title", "Bài 1")));

        JsonNode reply = handle(Access.READ_ONLY,
                "{\"jsonrpc\":\"2.0\",\"id\":\"a\",\"method\":\"tools/call\",\"params\":{\"name\":\"read_own_work\","
                        + "\"arguments\":{\"q\":\"bài 1\"},\"_meta\":{\"inputTokens\":120,\"outputTokens\":30}}}");

        assertThat(reply.get("id").asText()).isEqualTo("a");
        assertThat(reply.at("/result/isError").asBoolean()).isFalse();
        assertThat(reply.at("/result/content/0/text").asText()).contains("Bài 1");
        var records = ArgumentCaptor.forClass(edu.aiplatform.ai.model.AiUsageRecord.class);
        verify(usageRepository).insert(records.capture());
        assertThat(records.getValue().toolName()).isEqualTo("read_own_work");
        assertThat(records.getValue().inputTokens()).isEqualTo(120);
        assertThat(actions()).containsExactly("MCP_TOOL_CALLED");
    }

    @Test
    void argumentsMustMatchSchemaAndStayUnder64Kb() {
        assertThat(handle(Access.READ_ONLY, call("read_own_work", "{\"q\":1}")).at("/error/code").asInt())
                .isEqualTo(-32602);
        String big = "{\"q\":\"" + "a".repeat(64 * 1024) + "\"}";
        assertThat(handle(Access.READ_ONLY, call("read_own_work", big)).at("/error/message").asText()).contains("64 KB");
        verify(provider, never()).call(any(), anyString(), any());
    }

    @Test
    void toolNameMustBeShortText() {
        assertThat(handle(Access.READ_ONLY, call("x".repeat(101), "{\"q\":\"x\"}")).at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(handle(Access.READ_ONLY, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":5}}")
                .at("/error/code").asInt()).isEqualTo(-32602);
        verify(limiter, never()).tryAcquire(any());
    }

    @Test
    void rateLimitAndLimiterFailureRefuseTheCall() {
        when(limiter.tryAcquire(any())).thenReturn(false);
        assertThat(handle(Access.READ_ONLY, call("read_own_work", "{\"q\":\"x\"}")).at("/error/code").asInt())
                .isEqualTo(-32029);
        when(limiter.tryAcquire(any())).thenThrow(new IllegalStateException("redis down"));
        assertThat(handle(Access.READ_ONLY, call("read_own_work", "{\"q\":\"x\"}")).at("/error/code").asInt())
                .isEqualTo(-32029);
        verify(provider, never()).call(any(), anyString(), any());
    }

    @Test
    void ownerDenialAndResultSizeAreHandled() {
        when(provider.call(any(), anyString(), any())).thenThrow(ApiException.notFound("ASSIGNMENT_NOT_FOUND"));
        JsonNode denied = handle(Access.READ_ONLY, call("read_own_work", "{\"q\":\"x\"}"));
        assertThat(denied.at("/result/isError").asBoolean()).isTrue();
        assertThat(denied.at("/result/content/0/text").asText()).doesNotContain("ASSIGNMENT");

        org.mockito.Mockito.doReturn(new McpToolResult(false, "é".repeat(200_000))).when(provider)
                .call(any(), anyString(), any());
        var oversized = handle(Access.READ_ONLY, call("read_own_work", "{\"q\":\"x\"}"));
        assertThat(oversized.at("/result/isError").asBoolean()).isTrue();
        String text = oversized.at("/result/content/0/text").asText();
        assertThat(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(256 * 1024);
        assertThat(text).contains("MCP_RESULT_TOO_LARGE");
    }

    @Test void staleSnapshotReturnsMachineReadableErrorCodeAndEscapedResultsStayBounded() {
        when(provider.call(any(),anyString(),any())).thenThrow(ApiException.conflict("SUBMISSION_CHANGED","Restart reading"));
        var reply=handle(Access.READ_ONLY,call("read_own_work","{\"q\":\"x\"}"));
        assertThat(reply.at("/result/content/0/text").asText()).contains("SUBMISSION_CHANGED");
        org.mockito.Mockito.doReturn(new McpToolResult(false,"\"".repeat(160000))).when(provider).call(any(),anyString(),any());
        reply=handle(Access.READ_ONLY,call("read_own_work","{\"q\":\"x\"}"));
        assertThat(reply.at("/result/isError").asBoolean()).isTrue();
        assertThat(reply.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThan(256*1024);
    }

    private List<String> actions() {
        ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, org.mockito.Mockito.atLeastOnce()).record(events.capture());
        return events.getAllValues().stream().map(AuditEvent::action).toList();
    }

    private static String call(String tool, String args) {
        return "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool
                + "\",\"arguments\":" + args + "}}";
    }

    private JsonNode handle(Access access, String body) {
        ObjectNode reply = server.handle(principal(access), read(body)).orElseThrow();
        assertThat(reply.get("jsonrpc").asText()).isEqualTo("2.0");
        return reply;
    }

    private JsonNode read(String body) {
        try {
            return json.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private McpPrincipal principal(Access access) {
        return new McpPrincipal(new McpCredential(UUID.randomUUID(), teacher.accountId(), "agent", access, "h", "abcd",
                NOW.plusSeconds(60), null, null, NOW), teacher);
    }
}
