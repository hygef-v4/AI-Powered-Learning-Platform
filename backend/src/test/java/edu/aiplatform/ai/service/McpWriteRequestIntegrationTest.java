package edu.aiplatform.ai.service;

import edu.aiplatform.shared.testsupport.SharedPostgres;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import edu.aiplatform.ai.repository.McpWriteRequestRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true)
class McpWriteRequestIntegrationTest {
    static final SharedPostgres.Database db = SharedPostgres.createDatabase();
    static JdbcTemplate jdbc;
    static McpWriteRequestService service;
    final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    @BeforeAll static void setup() throws Exception {
        var source = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
        jdbc = new JdbcTemplate(source);
        jdbc.execute("create table accounts(id uuid primary key)");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/ai/V20261026_1800__create_mcp_write_requests.sql")));
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/ai/V20261123_1210__allow_mcp_write_request_cleanup.sql")));
        jdbc.execute("create table test_drafts(id uuid primary key)");
        service = new McpWriteRequestService(new McpWriteRequestRepository(JdbcClient.create(source)), new ObjectMapper(),
                new DataSourceTransactionManager(source));
    }
    @BeforeEach void account() { jdbc.update("insert into accounts values (?)", actor.accountId()); }
    @Test void runtimeRoleCanReadAndInsertRetryResults() {
        assertThat(jdbc.queryForObject("select has_table_privilege('app','mcp_write_requests','SELECT, INSERT, DELETE')",Boolean.class)).isTrue();
    }
    @Test void cleanupDeletesOnlyRecordsOlderThanSevenDays() {
        service.execute(actor,"create","old","same",() -> Map.of("id","old"));
        service.execute(actor,"create","recent","same",() -> Map.of("id","recent"));
        Instant now = Instant.now();
        jdbc.update("update mcp_write_requests set created_at=? where request_ref='old'",
                java.sql.Timestamp.from(now.minus(Duration.ofDays(8))));

        assertThat(service.deleteExpiredRequests(now)).isEqualTo(1);
        assertThat(jdbc.queryForList("select request_ref from mcp_write_requests where account_id=?",String.class,
                actor.accountId())).containsExactly("recent");
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
                    .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.readCode()).isEqualTo("MCP_REQUEST_CONFLICT"));
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
