package edu.aiplatform.ai.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.service.AiAccess;
import edu.aiplatform.ai.service.AiConnectionService;
import edu.aiplatform.ai.service.AiConnectionService.ConnectionView;
import edu.aiplatform.ai.service.AiConnectionService.SaveCommand;
import edu.aiplatform.ai.service.AiSourceService;
import edu.aiplatform.ai.service.AiTaskService;
import edu.aiplatform.ai.service.AiUsageService;
import edu.aiplatform.ai.service.AiUsageService.UsagePage;
import edu.aiplatform.ai.service.McpCredentialService;
import edu.aiplatform.ai.service.McpCredentialService.Created;
import edu.aiplatform.ai.service.McpCredentialService.CredentialView;
import edu.aiplatform.ai.service.McpCredentialService.McpPrincipal;
import edu.aiplatform.ai.service.McpServer;
import edu.aiplatform.ai.service.ProviderCatalog;
import edu.aiplatform.ai.service.QuotaPolicyService;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.McpCredential;
import edu.aiplatform.ai.model.McpCredential.Access;
import edu.aiplatform.ai.model.QuotaPolicy;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Mapping và tuần tự hóa của API U06; quyền thật nằm ở service (đã có unit test riêng). */
class AiControllersTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    private final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final AiConnectionService connections = mock(AiConnectionService.class);
    private final ProviderCatalog catalog = mock(ProviderCatalog.class);
    private final McpCredentialService credentials = mock(McpCredentialService.class);
    private final McpServer server = mock(McpServer.class);
    private final AiUsageService usage = mock(AiUsageService.class);
    private final AiSourceService sources = mock(AiSourceService.class);
    private final AiTaskService tasks = mock(AiTaskService.class);
    private final QuotaPolicyService quota = mock(QuotaPolicyService.class);
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        mvc = MockMvcBuilders.standaloneSetup(new AiConnectionController(connections, catalog),
                        new McpCredentialController(credentials), new AiUsageController(usage, sources, tasks),
                        new AiAdminController(connections, quota), new McpController(credentials, server, json))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void personalConnectionRoutesUseServerActorAndColonActions() throws Exception {
        ConnectionView view = new ConnectionView(AiProvider.CLAUDE, true, "claude-sonnet-5-5", "3kQf",
                AiConnection.Status.ACTIVE, null, null, 2, UUID.randomUUID());
        when(connections.savePersonal(eq(actor), eq(AiProvider.CLAUDE), any())).thenReturn(view);
        when(connections.testPersonal(actor, AiProvider.CLAUDE)).thenReturn(view);

        mvc.perform(put("/api/v1/me/ai-connections/claude").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk-ant-secret-0000\",\"model\":\"claude-sonnet-5-5\",\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.keyHint").value("3kQf"))
                .andExpect(content().string(not(containsString("secret"))));
        verify(connections).savePersonal(actor, AiProvider.CLAUDE,
                new SaveCommand("sk-ant-secret-0000", "claude-sonnet-5-5", 1L));
        mvc.perform(post("/api/v1/me/ai-connections/CLAUDE:test")).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/me/ai-connections/claude")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/me/ai-connections/mistral")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AiAccess.NOT_FOUND));
    }

    @Test
    void mcpCredentialCreateReturnsTokenOnce() throws Exception {
        CredentialView view = new CredentialView(UUID.randomUUID(), "Claude Desktop", Access.READ_ONLY, "AbCd", NOW,
                NOW.plusSeconds(86400), null, McpCredential.Status.ACTIVE, true);
        when(credentials.create(actor, "Claude Desktop", Access.READ_ONLY, 30)).thenReturn(new Created(view, "mcp_xyzAbCd"));
        when(credentials.list(actor)).thenReturn(List.of(view));

        mvc.perform(post("/api/v1/me/mcp-credentials").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Claude Desktop\",\"access\":\"READ_ONLY\",\"days\":30}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.token").value("mcp_xyzAbCd"))
                .andExpect(jsonPath("$.endpoint").value("/mcp"));
        mvc.perform(get("/api/v1/me/mcp-credentials")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenHint").value("AbCd"))
                .andExpect(content().string(not(containsString("mcp_xyz"))));
    }

    @Test
    void usageHistoryKeepsUnknownTokensNullAndRejectsUnknownSource() throws Exception {
        when(usage.listOwn(eq(actor), any(), any(), any(), eq(0), eq(50))).thenReturn(new UsagePage(List.of(
                new edu.aiplatform.ai.model.AiUsageRecord(UUID.randomUUID(), actor.accountId(),
                        edu.aiplatform.ai.model.AiUsageRecord.Source.MCP, null, UUID.randomUUID(), null, null, null,
                        "read_own_work", "OK", null, null, null, null,
                        edu.aiplatform.ai.contract.UsageMeasurement.UNAVAILABLE, NOW)),
                0, 50, 1, new long[] {0, 0}, new long[] {0, 0}, new long[] {1, 0}));

        mvc.perform(get("/api/v1/me/ai-usage")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].toolName").value("read_own_work"))
                .andExpect(jsonPath("$.items[0].inputTokens").isEmpty())
                .andExpect(jsonPath("$.mcp.requests").value(1))
                .andExpect(jsonPath("$.items[0].accountId").doesNotExist());
        mvc.perform(get("/api/v1/me/ai-usage").param("source", "BOGUS")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AI_USAGE_SOURCE_INVALID"));
        UUID task = UUID.randomUUID();
        mvc.perform(post("/api/v1/ai/tasks/" + task + ":cancel")).andExpect(status().isNoContent());
        verify(tasks).cancel(actor, task);
    }

    @Test
    void adminRoutesMapPolicyStatusAndQuotas() throws Exception {
        when(quota.savePolicy(actor, 20_000L, 500_000L, 3L)).thenReturn(new QuotaPolicy(20_000L, 500_000L, 4, null, NOW));
        mvc.perform(put("/api/v1/admin/ai/quota-policy").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultDayTokens\":20000,\"globalDayTokens\":500000,\"version\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.configured").value(true));
        UUID key = UUID.randomUUID();
        mvc.perform(put("/api/v1/admin/ai/connections/" + key + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false,\"version\":2}")).andExpect(status().isOk());
        verify(connections).setInstitutionalEnabled(actor, key, false, 2L);
        UUID teacher = UUID.randomUUID();
        mvc.perform(put("/api/v1/admin/ai/teacher-quotas/" + teacher).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dayTokens\":50000}")).andExpect(status().isNoContent());
        verify(quota).setTeacherQuota(actor, teacher, 50_000L);
        mvc.perform(post("/api/v1/admin/ai/connections/" + key + ":test")).andExpect(status().isOk());
        verify(connections).testInstitutional(actor, key);
        mvc.perform(post("/api/v1/admin/ai/connections").contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"openai\",\"apiKey\":\"sk-secret-0000\",\"model\":\"gpt-5\"}"))
                .andExpect(status().isOk());
        verify(connections).addInstitutional(eq(actor), eq(AiProvider.OPENAI), any());
    }

    @Test
    void mcpEndpointAuthenticatesBearerAndFollowsStreamableHttp() throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";
        mvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", containsString("Bearer")));
        when(credentials.authenticate("mcp_bad")).thenReturn(Optional.empty());
        mvc.perform(post("/mcp").header("Authorization", "Bearer mcp_bad").content(body))
                .andExpect(status().isUnauthorized());

        McpPrincipal principal = new McpPrincipal(new McpCredential(UUID.randomUUID(), actor.accountId(), "a",
                Access.READ_ONLY, "h", "abcd", NOW.plusSeconds(60), null, null, NOW), actor);
        when(credentials.authenticate("mcp_ok")).thenReturn(Optional.of(principal));
        ObjectNode reply = json.createObjectNode().put("jsonrpc", "2.0").put("id", 1);
        reply.putObject("result");
        when(server.handle(eq(principal), any())).thenReturn(Optional.of(reply));
        mvc.perform(post("/mcp").header("Authorization", "Bearer mcp_ok").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));

        when(server.handle(eq(principal), any())).thenReturn(Optional.empty());
        mvc.perform(post("/mcp").header("Authorization", "Bearer mcp_ok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")).andExpect(status().isAccepted());

        mvc.perform(post("/mcp").header("Authorization", "Bearer mcp_ok").header("Origin", "https://evil.test")
                .content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/mcp").header("Authorization", "Bearer mcp_ok").header("MCP-Protocol-Version", "1999-01-01")
                .content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/mcp")).andExpect(status().isMethodNotAllowed());
    }
}
