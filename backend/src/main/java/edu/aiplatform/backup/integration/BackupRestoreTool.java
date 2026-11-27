package edu.aiplatform.backup.integration;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import edu.aiplatform.backup.service.BackupEncryptor;
import edu.aiplatform.files.config.FilesProperties;
import edu.aiplatform.files.integration.DriveClientFactory;
import edu.aiplatform.files.integration.DriveFolders;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Tải bản khôi phục từ Google Drive (U03, 2026-11-26), không cần cơ sở dữ liệu cũ. Chọn base backup mới nhất tải lên
 * trước thời điểm cần khôi phục, tải các tệp WAL từ đó trở đi, giải mã nếu có khóa riêng, giải nén WAL. Kết quả trong
 * {@code <thư mục ra>}: {@code base.tar.gz} và thư mục {@code wal/}. Script {@code scripts/backup/restore-pitr.sh} (hướng dẫn: {@code scripts/backup/README.md}) dựng
 * Postgres cô lập từ kết quả này. Chạy trên máy người vận hành với cùng biến môi trường Drive như ứng dụng:
 * {@code java -cp app.jar -Dloader.main=edu.aiplatform.backup.integration.BackupRestoreTool
 * org.springframework.boot.loader.launch.PropertiesLauncher <thư mục ra> [--target-time <ISO-8601>]
 * [--private-key <tệp PEM>]}.
 */
public final class BackupRestoreTool {

    static final String ARCHIVE = "archive.alapbk";
    /** WAL tải lên trước khi thư mục base backup được tạo vẫn có thể cần cho đoạn đầu; lấy dư 1 giờ. */
    static final Duration WAL_MARGIN = Duration.ofHours(1);

    record DriveEntry(String id, String name, Instant createdAt, boolean folder) {
    }

    record Options(Path outDir, Instant targetTime, Path privateKey) {
    }

    private BackupRestoreTool() {
    }

    public static void main(String[] args) throws Exception {
        Options options;
        try {
            options = parseOptions(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Cách dùng: BackupRestoreTool <thư mục ra> [--target-time <ISO-8601>] [--private-key <tệp PEM>]");
            System.exit(2);
            return;
        }
        FilesProperties files = readFilesProperties();
        if (!files.hasDrive()) {
            System.err.println("Chưa có cấu hình Google Drive trong biến môi trường");
            System.exit(1);
        }
        DriveClientFactory clients = new DriveClientFactory(files);
        DriveFolders folders = new DriveFolders(clients, files);
        String backupFolder = resolveBackupFolder(folders, files);
        List<DriveEntry> children = listChildren(clients, files, backupFolder);
        // Lần sao lưu dở (đã tạo thư mục, chưa tải archive) thì lùi về base backup hợp lệ liền trước (review PR 148).
        DriveEntry base = null;
        DriveEntry archive = null;
        for (DriveEntry candidate : listBaseCandidates(children, options.targetTime())) {
            Optional<DriveEntry> found = findArchive(listChildren(clients, files, candidate.id()));
            if (found.isPresent()) {
                base = candidate;
                archive = found.get();
                break;
            }
            System.out.println("Bỏ qua thư mục " + candidate.name() + ": không có " + ARCHIVE);
        }
        if (base == null) {
            throw new IllegalStateException("Không có base backup đầy đủ nào trước thời điểm đã chọn");
        }
        System.out.println("Base backup: thư mục " + base.name() + ", tạo lúc " + base.createdAt());

        PrivateKey privateKey = options.privateKey() == null ? null
                : BackupEncryptor.parsePrivateKey(Files.readString(options.privateKey()));
        Files.createDirectories(options.outDir().resolve("wal"));
        Path downloaded = options.outDir().resolve(ARCHIVE);
        download(clients, archive.id(), downloaded);
        decryptTo(downloaded, options.outDir().resolve("base.tar.gz"), privateKey);
        Files.delete(downloaded);

        Optional<DriveEntry> walFolder = children.stream()
                .filter(entry -> entry.folder() && DriveBackupStore.WAL_FOLDER.equals(entry.name())).findFirst();
        List<DriveEntry> walFiles = walFolder.isEmpty() ? List.of()
                : selectWal(listChildren(clients, files, walFolder.get().id()), base.createdAt());
        for (DriveEntry wal : walFiles) {
            Path tmp = options.outDir().resolve("wal").resolve(wal.name());
            download(clients, wal.id(), tmp);
            restoreWalFile(tmp, options.outDir().resolve("wal").resolve(walName(wal.name())), privateKey);
            Files.delete(tmp);
        }
        System.out.println("Đã tải " + walFiles.size() + " tệp WAL vào " + options.outDir().resolve("wal"));
    }

    static Options parseOptions(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Thiếu thư mục ra");
        }
        Path out = Path.of(args[0]);
        Instant target = null;
        Path key = null;
        for (int i = 1; i < args.length; i++) {
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Thiếu giá trị cho " + args[i]);
            }
            switch (args[i]) {
                case "--target-time" -> target = OffsetDateTime.parse(args[++i]).toInstant();
                case "--private-key" -> key = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Không hiểu tham số " + args[i]);
            }
        }
        return new Options(out, target, key);
    }

    /** Thư mục base backup (tên là runId) mới nhất được tạo không muộn hơn {@code target}; không có target thì mới nhất. */
    static Optional<DriveEntry> selectBase(List<DriveEntry> children, Instant target) {
        return listBaseCandidates(children, target).stream().findFirst();
    }

    /** Các thư mục base backup không muộn hơn {@code target}, mới nhất trước, để lùi dần khi gặp thư mục dở. */
    static List<DriveEntry> listBaseCandidates(List<DriveEntry> children, Instant target) {
        return children.stream()
                .filter(DriveEntry::folder)
                .filter(entry -> !DriveBackupStore.WAL_FOLDER.equals(entry.name()))
                .filter(entry -> target == null || !entry.createdAt().isAfter(target))
                .sorted(Comparator.comparing(DriveEntry::createdAt).reversed())
                .toList();
    }

    /** Tệp {@code archive.alapbk} trong thư mục base backup, nếu lần sao lưu đã tải xong. */
    static Optional<DriveEntry> findArchive(List<DriveEntry> baseChildren) {
        return baseChildren.stream().filter(entry -> !entry.folder() && ARCHIVE.equals(entry.name())).findFirst();
    }

    /** Tệp WAL tải lên từ trước base backup một khoảng an toàn trở đi, theo thứ tự tên. */
    static List<DriveEntry> selectWal(List<DriveEntry> walFiles, Instant baseCreatedAt) {
        Instant from = baseCreatedAt.minus(WAL_MARGIN);
        return walFiles.stream()
                .filter(entry -> !entry.folder())
                .filter(entry -> !entry.createdAt().isBefore(from))
                .sorted(Comparator.comparing(DriveEntry::name))
                .toList();
    }

    /** {@code 000000010000000000000003.gz.alapbk} hoặc {@code ...gz} thành tên tệp WAL gốc. */
    static String walName(String uploadedName) {
        String name = uploadedName.endsWith(".alapbk") ? uploadedName.substring(0, uploadedName.length() - 7) : uploadedName;
        return name.endsWith(".gz") ? name.substring(0, name.length() - 3) : name;
    }

    static void restoreWalFile(Path downloaded, Path target, PrivateKey privateKey) throws IOException {
        Path compressed = downloaded.resolveSibling(downloaded.getFileName() + ".plain");
        decryptTo(downloaded, compressed, privateKey);
        try (InputStream in = new GZIPInputStream(Files.newInputStream(compressed));
             OutputStream out = Files.newOutputStream(target)) {
            in.transferTo(out);
        } finally {
            Files.deleteIfExists(compressed);
        }
    }

    static void decryptTo(Path source, Path target, PrivateKey privateKey) throws IOException {
        if (!BackupEncryptor.isEncrypted(source)) {
            Files.copy(source, target);
            return;
        }
        if (privateKey == null) {
            throw new IllegalStateException("Bản backup đã mã hóa, cần --private-key");
        }
        try (InputStream in = Files.newInputStream(source); OutputStream out = Files.newOutputStream(target)) {
            BackupEncryptor.decryptArchive(in, out, privateKey);
        }
    }

    private static String resolveBackupFolder(DriveFolders folders, FilesProperties files) {
        String configured = System.getenv("GOOGLE_BACKUP_FOLDER_ID");
        if (files.hasSharedDrive() && configured != null && !configured.isBlank()) {
            return configured.strip();
        }
        return folders.findFolder(folders.resolveRoot(), "backups")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thư mục backups trên Drive"));
    }

    private static List<DriveEntry> listChildren(DriveClientFactory clients, FilesProperties files, String parentId) {
        List<DriveEntry> entries = new ArrayList<>();
        String pageToken = null;
        do {
            String token = pageToken;
            FileList page = DriveClientFactory.runWithRetry(() -> {
                Drive.Files.List list = clients.createDrive().files().list()
                        .setQ("'" + parentId + "' in parents and trashed = false")
                        .setFields("nextPageToken, files(id, name, mimeType, createdTime)")
                        .setPageSize(1000).setPageToken(token);
                if (files.hasSharedDrive()) {
                    list.setCorpora("drive").setDriveId(files.sharedDriveId()).setIncludeItemsFromAllDrives(true)
                            .setSupportsAllDrives(true);
                }
                return list.execute();
            });
            if (page.getFiles() != null) {
                for (File file : page.getFiles()) {
                    entries.add(new DriveEntry(file.getId(), file.getName(),
                            Instant.ofEpochMilli(file.getCreatedTime().getValue()),
                            DriveFolders.FOLDER_MIME.equals(file.getMimeType())));
                }
            }
            pageToken = page.getNextPageToken();
        } while (pageToken != null);
        return entries;
    }

    private static void download(DriveClientFactory clients, String fileId, Path target) throws IOException {
        try (OutputStream out = Files.newOutputStream(target)) {
            clients.createDrive().files().get(fileId).setSupportsAllDrives(true).executeMediaAndDownloadTo(out);
        }
    }

    private static FilesProperties readFilesProperties() {
        return new FilesProperties(null, null, null, System.getenv("GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY"),
                System.getenv("GOOGLE_SHARED_DRIVE_ID"), System.getenv("GOOGLE_DRIVE_OAUTH_CLIENT_ID"),
                System.getenv("GOOGLE_DRIVE_OAUTH_CLIENT_SECRET"), System.getenv("GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN"),
                System.getenv("GOOGLE_DRIVE_FOLDER_NAME"), System.getenv("GOOGLE_DRIVE_FOLDER_ID"));
    }
}
