package edu.aiplatform.shared.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Một container PostgreSQL dùng chung cho mọi lớp test trong cùng JVM; mỗi fork surefire có container riêng.
 * Bật container và chạy initdb tốn 10–20 s trên Docker Desktop, nên chỉ làm một lần cho mỗi JVM.
 *
 * <p>Mỗi lớp test nhận một database mới và trống ({@link #createDatabase()}), nên dữ liệu, schema và lịch sử
 * Flyway không lẫn giữa các lớp, chạy theo thứ tự nào cũng như nhau. Role là đối tượng của cả cluster: role
 * {@code migrator}, {@code app}, {@code backup_replicator} được tạo một lần theo chính script
 * {@code infra/postgres/init/01-roles.sh}; test không tự tạo role nữa. Container không dừng thủ công, Ryuk của
 * Testcontainers dọn khi JVM kết thúc.
 */
public final class SharedPostgres {

    private static final String IMAGE = "postgres:16-alpine";
    private static final Path ROLES_SCRIPT = Path.of("..", "infra", "postgres", "init", "01-roles.sh");
    // Mỗi lớp giữ pool riêng, context Spring được cache còn giữ kết nối; mức mặc định 100 không đủ cho cả JVM.
    private static final int MAX_CONNECTIONS = 300;
    private static final Pattern HEREDOC = Pattern.compile("<<SQL\\R(.*?)\\RSQL\\b", Pattern.DOTALL);
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([A-Z_]+)(?::-([^}]*))?}");

    // Sinh mỗi lần chạy: container chỉ sống trong test, không để chuỗi mật khẩu cố định trong mã.
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private static PostgreSQLContainer<?> container;
    private static List<String> databaseGrants;

    private SharedPostgres() {
    }

    /** Database trống như database mặc định của một container mới; role của cluster đã có sẵn. */
    public static Database createDatabase() {
        return new Database(nameDatabase(), false);
    }

    /** Database đã chạy phần quyền theo database của {@code 01-roles.sh}, như volume khởi tạo khi triển khai. */
    public static Database createDatabaseWithRoles() {
        return new Database(nameDatabase(), true);
    }

    private static String nameDatabase() {
        String caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames
                        .map(StackWalker.StackFrame::getDeclaringClass)
                        .filter(type -> type != SharedPostgres.class)
                        .findFirst()
                        .map(Class::getSimpleName)
                        .orElse("test"));
        String label = caller.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return "t" + SEQUENCE.incrementAndGet() + "_" + label.substring(0, Math.min(label.length(), 40));
    }

    private static synchronized PostgreSQLContainer<?> startContainer() {
        if (container == null) {
            PostgreSQLContainer<?> started = new PostgreSQLContainer<>(IMAGE)
                    .withPassword(UUID.randomUUID().toString())
                    .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=" + MAX_CONNECTIONS);
            started.start();
            List<String> grants = new ArrayList<>();
            try (Connection connection = DriverManager.getConnection(
                    started.getJdbcUrl(), started.getUsername(), started.getPassword());
                    Statement statement = connection.createStatement()) {
                for (String sql : readRolesStatements()) {
                    if (sql.regionMatches(true, 0, "CREATE ROLE", 0, "CREATE ROLE".length())) {
                        statement.execute(sql);
                    } else {
                        grants.add(sql);
                    }
                }
            } catch (SQLException exception) {
                started.stop();
                throw new IllegalStateException("Không tạo được role dùng chung", exception);
            }
            databaseGrants = List.copyOf(grants);
            container = started;
        }
        return container;
    }

    /** Câu SQL trong heredoc của script triển khai; {@code ${POSTGRES_DB}} giữ lại để thay theo từng database. */
    private static List<String> readRolesStatements() {
        String script;
        try {
            script = Files.readString(ROLES_SCRIPT).replace("\r", "");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        Matcher heredoc = HEREDOC.matcher(script);
        if (!heredoc.find()) {
            throw new IllegalStateException("Không tìm thấy khối SQL trong " + ROLES_SCRIPT);
        }
        Map<String, String> variables = Map.of(
                "POSTGRES_APP_PASSWORD", APP_PASSWORD,
                "POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD,
                "POSTGRES_DB", "${POSTGRES_DB}");
        Matcher variable = VARIABLE.matcher(heredoc.group(1));
        StringBuilder sql = new StringBuilder();
        while (variable.find()) {
            String fallback = variable.group(2) == null ? "" : variable.group(2);
            String value = variables.getOrDefault(variable.group(1), fallback);
            variable.appendReplacement(sql, Matcher.quoteReplacement(value));
        }
        variable.appendTail(sql);
        List<String> statements = new ArrayList<>();
        for (String part : sql.toString().split(";")) {
            String statement = part.strip();
            if (!statement.isEmpty()) {
                statements.add(statement);
            }
        }
        return statements;
    }

    /** Một database riêng của một lớp test; chỉ tạo khi được dùng lần đầu, để lớp bị tắt vì thiếu Docker vẫn bỏ qua. */
    public static final class Database {

        private final String name;
        private final boolean withRoles;
        private boolean created;

        private Database(String name, boolean withRoles) {
            this.name = name;
            this.withRoles = withRoles;
        }

        public String getJdbcUrl() {
            PostgreSQLContainer<?> server = createIfMissing();
            return "jdbc:postgresql://" + server.getHost() + ":"
                    + server.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + name + "?loggerLevel=OFF";
        }

        /** Tài khoản superuser của container, như {@code getUsername()} của một container riêng. */
        public String getUsername() {
            return createIfMissing().getUsername();
        }

        public String getPassword() {
            return createIfMissing().getPassword();
        }

        public String getDatabaseName() {
            return name;
        }

        /** Mật khẩu role {@code app}; đăng nhập được trong database tạo bằng {@link #createDatabaseWithRoles()}. */
        public String getAppPassword() {
            return APP_PASSWORD;
        }

        /** Mật khẩu role {@code migrator}; có quyền tạo schema trong database tạo bằng {@link #createDatabaseWithRoles()}. */
        public String getMigratorPassword() {
            return MIGRATOR_PASSWORD;
        }

        private synchronized PostgreSQLContainer<?> createIfMissing() {
            PostgreSQLContainer<?> server = startContainer();
            if (!created) {
                try (Connection connection = DriverManager.getConnection(
                        server.getJdbcUrl(), server.getUsername(), server.getPassword());
                        Statement statement = connection.createStatement()) {
                    statement.execute("CREATE DATABASE " + name);
                } catch (SQLException exception) {
                    throw new IllegalStateException("Không tạo được database " + name, exception);
                }
                if (withRoles) {
                    applyGrants(server);
                }
                created = true;
            }
            return server;
        }

        private void applyGrants(PostgreSQLContainer<?> server) {
            String url = server.getJdbcUrl().replaceFirst("/" + server.getDatabaseName() + "(?=\\?|$)", "/" + name);
            try (Connection connection = DriverManager.getConnection(url, server.getUsername(), server.getPassword());
                    Statement statement = connection.createStatement()) {
                for (String grant : databaseGrants) {
                    statement.execute(grant.replace("${POSTGRES_DB}", name));
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Không cấp được quyền cho database " + name, exception);
            }
        }
    }
}
