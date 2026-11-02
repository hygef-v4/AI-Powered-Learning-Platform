package edu.aiplatform.backup.infrastructure;

import edu.aiplatform.backup.application.BackupEncryptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Giải mã archive khi khôi phục (runbook {@code docs/runbooks/restore-isolated.md}), chạy trên máy người vận hành:
 * {@code java -cp app.jar -Dloader.main=edu.aiplatform.backup.infrastructure.BackupDecryptTool
 * org.springframework.boot.loader.launch.PropertiesLauncher <archive> <private-key.pem> <dump.pgc>}.
 */
public final class BackupDecryptTool {

    private BackupDecryptTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Cách dùng: BackupDecryptTool <archive> <private-key.pem> <dump.pgc>");
            System.exit(2);
        }
        var privateKey = BackupEncryptor.parsePrivateKey(Files.readString(Path.of(args[1])));
        try (InputStream in = Files.newInputStream(Path.of(args[0]));
             OutputStream out = Files.newOutputStream(Path.of(args[2]))) {
            BackupEncryptor.decrypt(in, out, privateKey);
        }
        System.out.println("Đã giải mã " + args[0] + " -> " + args[2]);
    }
}
