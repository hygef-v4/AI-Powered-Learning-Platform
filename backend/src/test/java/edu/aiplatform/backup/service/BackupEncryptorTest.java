package edu.aiplatform.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BackupEncryptorTest {

    static KeyPair keys;
    static KeyPair otherKeys;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        otherKeys = generator.generateKeyPair();
    }

    static byte[] encrypt(byte[] plain) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BackupEncryptor.encrypt(new ByteArrayInputStream(plain), out, keys.getPublic());
        return out.toByteArray();
    }

    static byte[] decrypt(byte[] archive, KeyPair pair) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BackupEncryptor.decrypt(new ByteArrayInputStream(archive), out, pair.getPrivate());
        return out.toByteArray();
    }

    @Test
    void roundTripsEmptySmallAndMultiChunkData() throws Exception {
        byte[] large = new byte[BackupEncryptor.CHUNK_SIZE * 2 + 123];
        new Random(42).nextBytes(large);
        for (byte[] plain : new byte[][]{new byte[0], "dump nhỏ".getBytes(), large}) {
            assertThat(decrypt(encrypt(plain), keys)).isEqualTo(plain);
        }
    }

    @Test
    void archiveDoesNotContainThePlainText() throws Exception {
        byte[] plain = "INSERT INTO accounts VALUES ('secret-row')".getBytes();
        assertThat(new String(encrypt(plain), java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("secret-row");
    }

    @Test
    void rejectsTamperedTruncatedOrWrongKey() throws Exception {
        byte[] plain = new byte[BackupEncryptor.CHUNK_SIZE + 10];
        byte[] archive = encrypt(plain);

        byte[] tampered = archive.clone();
        tampered[tampered.length - 5] ^= 1;
        assertThatThrownBy(() -> decrypt(tampered, keys)).isInstanceOf(IOException.class);

        byte[] truncated = Arrays.copyOf(archive, archive.length - (10 + 16 + 4));
        assertThatThrownBy(() -> decrypt(truncated, keys)).isInstanceOf(IOException.class);

        assertThatThrownBy(() -> decrypt(archive, otherKeys)).isInstanceOf(IOException.class);
    }

    @Test
    void parsesPemKeys() throws Exception {
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keys.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n";
        assertThat(BackupEncryptor.parsePublicKey(pem)).isEqualTo(keys.getPublic());
        String privatePem = Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded());
        assertThat(BackupEncryptor.parsePrivateKey(privatePem)).isEqualTo(keys.getPrivate());
    }
}
