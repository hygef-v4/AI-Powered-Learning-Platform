package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true)
class McpWriteRequestIntegrationTest {
    @Container static final PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");
    static JdbcTemplate jdbc;
    static McpWriteRequestService service;
    final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    @BeforeAll static void setup() throws Exception {
        var source = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        jdbc = new JdbcTemplate(source);
        jdbc.execute("create role app");
        jdbc.execute("create table accounts(id uuid primary key)");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/ai/V20261026_1800__create_mcp_write_requests.sql")));
        jdbc.execute("create table test_drafts(id uuid primary key)");
        service = new McpWriteRequestService(jdbc, new ObjectMapper(), new DataSourceTransactionManager(source));
    }
    @BeforeEach void account() { jdbc.update("insert into accounts values (?)", actor.accountId()); }
    @Test void runtimeRoleCanReadAndInsertRetryResults() {
        assertThat(jdbc.queryForObject("select has_table_privilege('app','mcp_write_requests','SELECT, INSERT')",Boolean.class)).isTrue();
    }
    @Test void concurrentRetryCreatesOneDraftAndReturnsSameResult() throws Exception {
        var count = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Map<String,Object>>> calls = new ArrayList<>();
            for(int n=0;n<4;n++) calls.add(pool.submit(() -> service.execute(actor,"create","retry","same",() -> {
                count.incrementAndGet(); var id=UUID.randomUUID().toString();
                jdbc.update("insert into test_drafts values (?::uuid)",id); return Map.of("id",id);
            })));
            var first=calls.get(0).get(20,TimeUnit.SECONDS);
            for(var call:calls) assertThat(call.get(20,TimeUnit.SECONDS)).isEqualTo(first);
            assertThat(count.get()).isEqualTo(1);
            assertThatThrownBy(() -> service.execute(actor,"create","retry","changed",Map::of))
                    .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("MCP_REQUEST_CONFLICT"));
        } finally { pool.shutdownNow(); }
    }
    @Test void failedMutationRollsBackBothDraftAndRetryRecord() {
        UUID id=UUID.randomUUID();
        assertThatThrownBy(() -> service.execute(actor,"create","rollback","same",() -> {
            jdbc.update("insert into test_drafts values (?)",id); throw new IllegalStateException("reject");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from test_drafts where id=?",Integer.class,id)).isZero();
        assertThat(service.execute(actor,"create","rollback","same",() -> Map.of("id","retry-ok")))
                .containsEntry("id","retry-ok");
    }
}
