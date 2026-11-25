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
        server = new McpServer(new McpToolRegistry(providers), limiter, new JsonSchemaChecker(json), usage, json, clock);
        when(limiter.tryAcquire(any())).thenReturn(true);
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
    void argumentsMustMatchSchemaAndStayUnder64Kb() {
        assertThat(handle(Access.READ_ONLY, call("read_own_work", "{\"q\":1}")).at("/error/code").asInt())
                .isEqualTo(-32602);
        String big = "{\"q\":\"" + "a".repeat(64 * 1024) + "\"}";
        assertThat(handle(Access.READ_ONLY, call("read_own_work", big)).at("/error/message").asText()).contains("64 KB");
        assertThat(handle(Access.READ_ONLY, call("read_own_work", "[1]")).at("/error/code").asInt()).isEqualTo(-32602);
        verify(provider, never()).call(any(), anyString(), any());
        var events = ArgumentCaptor.forClass(AuditEvent.class);
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
        // Still refused (fail closed), but with its own code so callers are not told they sent too many calls.
        var unavailable = handle(Access.READ_ONLY, call("read_own_work", "{\"q\":\"x\"}"));
        assertThat(unavailable.at("/error/code").asInt()).isEqualTo(-32030);
        assertThat(unavailable.at("/error/message").asText()).doesNotContain("60");
        verify(provider, never()).call(any(), anyString(), any());
    }

    private List<String> actions() {
        ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
        return events.getAllValues().stream().map(AuditEvent::action).toList();
    }

    private static String call(String tool, String args) {
        return "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool
                + "\",\"arguments\":" + args + "}}";
    }

    private JsonNode handle(Access access, String body) {
        ObjectNode reply = server.handleMessage(principal(access), read(body)).orElseThrow();
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
