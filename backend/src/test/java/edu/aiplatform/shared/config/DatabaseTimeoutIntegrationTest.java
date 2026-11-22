package edu.aiplatform.shared.config;

import static org.assertj.core.api.Assertions.*;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Loads the production pool settings, then proves cancellation and pool recovery against PostgreSQL. */
@Testcontainers(disabledWithoutDocker = true)
class DatabaseTimeoutIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void configuredPoolBoundsLocksAndQueriesAndRecoversAfterRollback() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withInitializer(context -> {
                    try {
                        new YamlPropertySourceLoader().load("production", new ClassPathResource("application.yml"))
                                .forEach(source -> context.getEnvironment().getPropertySources().addLast(source));
                    } catch (java.io.IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .withPropertyValues("spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DataSource source = context.getBean(DataSource.class);
                    JdbcTemplate jdbc = new JdbcTemplate(source);
                    assertThat(jdbc.queryForObject("show statement_timeout", String.class)).isEqualTo("15s");
                    assertThat(jdbc.queryForObject("show lock_timeout", String.class)).isEqualTo("2s");
                    var pool = (HikariDataSource) source;
                    assertThat(pool.getConnectionTimeout()).isLessThanOrEqualTo(3000);
                    jdbc.execute("create table nfr_lock(id int primary key, value int not null)");
                    jdbc.update("insert into nfr_lock values(1, 0)");
                    TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(source));
                    try (Connection blocker = source.getConnection()) {
                        blocker.setAutoCommit(false);
                        try (var update = blocker.createStatement()) {
                            update.executeUpdate("update nfr_lock set value=1 where id=1");
                        }
                        long started = System.nanoTime();
                        assertThatThrownBy(() -> tx.executeWithoutResult(status ->
                                jdbc.update("update nfr_lock set value=2 where id=1")))
                                .isInstanceOf(DataAccessException.class)
                                .rootCause().isInstanceOf(java.sql.SQLException.class)
                                .satisfies(error -> assertThat(((java.sql.SQLException) error).getSQLState()).isEqualTo("55P03"));
                        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(6));
                        blocker.rollback();
                    }
                    assertThat(jdbc.queryForObject("select value from nfr_lock", Integer.class)).isZero();
                    long started = System.nanoTime();
                    assertThatThrownBy(() -> tx.executeWithoutResult(status -> jdbc.execute("select pg_sleep(20)")))
                            .isInstanceOf(QueryTimeoutException.class);
                    assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(19));
                    tx.executeWithoutResult(status -> jdbc.update("update nfr_lock set value=3 where id=1"));
                    assertThat(jdbc.queryForObject("select value from nfr_lock", Integer.class)).isEqualTo(3);
                });
    }
}
