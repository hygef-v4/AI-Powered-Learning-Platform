package edu.aiplatform.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.backup.config.BackupProperties;
import edu.aiplatform.backup.integration.LocalBackupStore;
import edu.aiplatform.backup.repository.WalSegmentRepository;
import edu.aiplatform.files.service.StorageUnavailableException;
import edu.aiplatform.settings.contract.ISettings;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WalShipScannerTest {

    static final String WAL_1 = "000000010000000000000001";
    static final String WAL_2 = "000000010000000000000002";

    @TempDir
    Path dir;

    final WalSegmentRepository segments = mock(WalSegmentRepository.class);
    final ISettings settings = mock(ISettings.class);

    @org.junit.jupiter.api.BeforeEach
    void enableBackup() {
        when(settings.get(BackupSettingDefinitions.ENABLED)).thenReturn(java.util.Optional.of(true));
    }

    WalShipScanner scanner(BackupProperties props, IBackupStore store) {
        return new WalShipScanner(props, store, segments, settings,
                mock(org.springframework.transaction.PlatformTransactionManager.class));
    }
    final Instant now = Instant.parse("2026-11-26T07:00:00Z");

    BackupProperties props(String publicKey) {
        return new BackupProperties(null, publicKey, "localhost", 5432, "platform", "backup_replicator", "",
                dir.resolve("tmp").toString(), dir.resolve("wal-archive").toString(), null);
    }

    Path walDir() throws Exception {
        return Files.createDirectories(dir.resolve("wal-archive"));
    }

    @Test
    void uploadsCompressedWalInNameOrderRecordsItAndDeletesTheLocalCopy() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        Files.writeString(walDir().resolve(WAL_2), "second");
        Files.writeString(walDir().resolve(WAL_1), "first");
        LocalBackupStore store = new LocalBackupStore(dir.resolve("drive"));

        scanner(props(null), store).scan(now);

        Path uploaded = dir.resolve("drive").resolve("wal").resolve(WAL_1 + ".gz");
        try (InputStream in = new GZIPInputStream(Files.newInputStream(uploaded))) {
            assertThat(new String(in.readAllBytes())).isEqualTo("first");
        }
        var order = org.mockito.Mockito.inOrder(segments);
        order.verify(segments).recordUploaded(eq(WAL_1), eq(WAL_1 + ".gz"), anyLong(), eq(now));
        order.verify(segments).recordUploaded(eq(WAL_2), eq(WAL_2 + ".gz"), anyLong(), eq(now));
        assertThat(walDir()).isEmptyDirectory();
    }

    @Test
    void encryptsWalWhenAPublicKeyIsSet() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        Files.writeString(walDir().resolve(WAL_1), "secret change");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
        LocalBackupStore store = new LocalBackupStore(dir.resolve("drive"));

        scanner(props(publicKey), store).scan(now);

        Path uploaded = dir.resolve("drive").resolve("wal").resolve(WAL_1 + ".gz.alapbk");
        assertThat(BackupEncryptor.isEncrypted(uploaded)).isTrue();
    }

    @Test
    void skipsFilesStillBeingCopiedAndUnknownNames() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        Files.writeString(walDir().resolve(WAL_1 + ".tmp"), "copying");
        Files.writeString(walDir().resolve("notes.txt"), "x");
        IBackupStore store = mock(IBackupStore.class);
        when(store.isAvailable()).thenReturn(true);

        scanner(props(null), store).scan(now);

        verify(store, never()).putWal(anyString(), any());
        assertThat(walDir().resolve(WAL_1 + ".tmp")).exists();
    }

    @Test
    void keepsTheFileAndStopsWhenDriveFails() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        Files.writeString(walDir().resolve(WAL_1), "first");
        Files.writeString(walDir().resolve(WAL_2), "second");
        IBackupStore store = mock(IBackupStore.class);
        when(store.isAvailable()).thenReturn(true);
        when(store.putWal(anyString(), any())).thenThrow(new StorageUnavailableException("down", null));

        scanner(props(null), store).scan(now);

        verify(store).putWal(eq(WAL_1 + ".gz"), any());
        verify(segments, never()).recordUploaded(anyString(), anyString(), anyLong(), any());
        assertThat(walDir().resolve(WAL_1)).exists();
        assertThat(walDir().resolve(WAL_2)).exists();
    }

    @Test
    void deletesWalWithoutStoringItWhenGoogleDriveIsNotConfigured() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        Files.writeString(walDir().resolve(WAL_1), "first");
        IBackupStore store = mock(IBackupStore.class);
        when(store.isAvailable()).thenReturn(false);

        scanner(props(null), store).scan(now);

        verify(store, never()).putWal(anyString(), any());
        assertThat(walDir()).isEmptyDirectory();
    }

    @Test
    void deletesWalWithoutStoringItWhenBackupIsTurnedOff() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        when(settings.get(BackupSettingDefinitions.ENABLED)).thenReturn(java.util.Optional.of(false));
        Files.writeString(walDir().resolve(WAL_1), "first");
        IBackupStore store = mock(IBackupStore.class);
        when(store.isAvailable()).thenReturn(true);

        scanner(props(null), store).scan(now);

        verify(store, never()).putWal(anyString(), any());
        assertThat(walDir()).isEmptyDirectory();
    }

    @Test
    void doesNothingWhenAnotherWorkerHoldsTheLock() throws Exception {
        when(segments.tryLockShipping()).thenReturn(false);
        Files.writeString(walDir().resolve(WAL_1), "first");
        IBackupStore store = mock(IBackupStore.class);

        scanner(props(null), store).scan(now);

        verify(store, never()).putWal(anyString(), any());
        assertThat(walDir().resolve(WAL_1)).exists();
    }

    @Test
    void mismatchedUploadIsDeletedFromDriveAndTheLocalFileKept() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        Files.writeString(walDir().resolve(WAL_1), "first");
        IBackupStore store = mock(IBackupStore.class);
        when(store.isAvailable()).thenReturn(true);
        when(store.putWal(anyString(), any())).thenReturn(new IBackupStore.StoredArchive("ref-1", 1L, null));

        scanner(props(null), store).scan(now);

        verify(store).deleteWal("ref-1");
        verify(segments, never()).recordUploaded(anyString(), anyString(), anyLong(), any());
        assertThat(walDir().resolve(WAL_1)).exists();
    }

    @Test
    void failureOnALaterFileKeepsTheEarlierFileShipped() throws Exception {
        when(segments.tryLockShipping()).thenReturn(true);
        Files.writeString(walDir().resolve(WAL_1), "first");
        Files.writeString(walDir().resolve(WAL_2), "second");
        LocalBackupStore drive = new LocalBackupStore(dir.resolve("drive"));
        IBackupStore store = mock(IBackupStore.class);
        when(store.isAvailable()).thenReturn(true);
        when(store.putWal(eq(WAL_1 + ".gz"), any())).thenAnswer(i -> drive.putWal(i.getArgument(0), i.getArgument(1)));
        when(store.putWal(eq(WAL_2 + ".gz"), any())).thenThrow(new StorageUnavailableException("down", null));

        scanner(props(null), store).scan(now);

        verify(segments).recordUploaded(eq(WAL_1), eq(WAL_1 + ".gz"), anyLong(), eq(now));
        assertThat(walDir().resolve(WAL_1)).doesNotExist();
        assertThat(walDir().resolve(WAL_2)).exists();
    }

    @Test
    void recognisesWalHistoryAndBackupLabelFiles() {
        assertThat(WalShipScanner.WAL_FILE.matcher(WAL_1).matches()).isTrue();
        assertThat(WalShipScanner.WAL_FILE.matcher("00000002.history").matches()).isTrue();
        assertThat(WalShipScanner.WAL_FILE.matcher(WAL_1 + ".00000028.backup").matches()).isTrue();
        assertThat(WalShipScanner.WAL_FILE.matcher(WAL_1 + ".tmp").matches()).isFalse();
    }
}
