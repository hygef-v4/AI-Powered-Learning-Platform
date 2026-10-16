package edu.aiplatform.backup.application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mã hóa lai archive backup (P17, BR-U03-93): khóa AES-256 ngẫu nhiên mỗi lần chạy, bọc bằng public key RSA-OAEP
 * (SHA-256) của người vận hành; dữ liệu chia khối 1 MiB, mỗi khối AES-GCM riêng, AAD gồm số thứ tự khối và cờ khối
 * cuối nên không đảo, cắt hay ghép khối được. Giải mã chỉ làm được với private key giữ ngoài VPS.
 *
 * <p>Định dạng: {@code "ALAPBK01"}, độ dài khóa bọc (int), khóa bọc, tiền tố nonce 4 byte, rồi các khối
 * {@code [độ dài (int)][ciphertext + tag]}.
 */
public final class BackupEncryptor {

    static final byte[] MAGIC = "ALAPBK01".getBytes(StandardCharsets.US_ASCII);
    static final int CHUNK_SIZE = 1024 * 1024;
    static final int TAG_BITS = 128;
    static final OAEPParameterSpec OAEP = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT);

    private BackupEncryptor() {
    }

    public static void encrypt(InputStream in, OutputStream out, PublicKey publicKey) throws IOException {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            SecretKey key = generator.generateKey();
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
            rsa.init(Cipher.WRAP_MODE, publicKey, OAEP);
            byte[] wrapped = rsa.wrap(key);
            byte[] prefix = new byte[4];
            new SecureRandom().nextBytes(prefix);

            DataOutputStream data = new DataOutputStream(out);
            data.write(MAGIC);
            data.writeInt(wrapped.length);
            data.write(wrapped);
            data.write(prefix);

            byte[] current = readChunk(in);
            long index = 0;
            while (true) {
                byte[] next = readChunk(in);
                boolean last = next.length == 0;
                byte[] cipherText = aes(Cipher.ENCRYPT_MODE, key, prefix, index, last, current);
                data.writeInt(cipherText.length);
                data.write(cipherText);
                if (last) {
                    break;
                }
                current = next;
                index++;
            }
            data.flush();
        } catch (GeneralSecurityException e) {
            throw new IOException("Không mã hóa được archive", e);
        }
    }

    public static void decrypt(InputStream in, OutputStream out, PrivateKey privateKey) throws IOException {
        try {
            DataInputStream data = new DataInputStream(in);
            byte[] magic = data.readNBytes(MAGIC.length);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new IOException("Không phải archive backup");
            }
            byte[] wrapped = data.readNBytes(data.readInt());
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
            rsa.init(Cipher.UNWRAP_MODE, privateKey, OAEP);
            SecretKey key = (SecretKey) rsa.unwrap(wrapped, "AES", Cipher.SECRET_KEY);
            byte[] prefix = data.readNBytes(4);

            byte[] current = readBlock(data);
            if (current == null) {
                throw new IOException("Archive không có dữ liệu");
            }
            long index = 0;
            while (true) {
                byte[] next = readBlock(data);
                boolean last = next == null;
                out.write(aes(Cipher.DECRYPT_MODE, key, prefix, index, last, current));
                if (last) {
                    break;
                }
                current = next;
                index++;
            }
            out.flush();
        } catch (GeneralSecurityException e) {
            throw new IOException("Archive bị sửa hoặc sai khóa", e);
        }
    }

    public static PublicKey parsePublicKey(String pemOrBase64) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der(pemOrBase64)));
    }

    public static PrivateKey parsePrivateKey(String pemOrBase64) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der(pemOrBase64)));
    }

    private static byte[] der(String pemOrBase64) {
        String body = pemOrBase64.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static byte[] aes(int mode, SecretKey key, byte[] prefix, long index, boolean last, byte[] input)
            throws GeneralSecurityException {
        byte[] nonce = ByteBuffer.allocate(12).put(prefix).putLong(index).array();
        byte[] aad = ByteBuffer.allocate(9).putLong(index).put((byte) (last ? 1 : 0)).array();
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(mode, new SecretKeySpec(key.getEncoded(), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        aes.updateAAD(aad);
        return aes.doFinal(input);
    }

    private static byte[] readChunk(InputStream in) throws IOException {
        return in.readNBytes(CHUNK_SIZE);
    }

    private static byte[] readBlock(DataInputStream data) throws IOException {
        int length;
        try {
            length = data.readInt();
        } catch (EOFException e) {
            return null;
        }
        if (length < 0 || length > CHUNK_SIZE + TAG_BITS / 8) {
            throw new IOException("Khối archive không hợp lệ");
        }
        byte[] block = data.readNBytes(length);
        if (block.length != length) {
            throw new IOException("Archive bị cắt");
        }
        return block;
    }
}
