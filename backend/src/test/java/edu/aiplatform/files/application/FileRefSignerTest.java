package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileRefSignerTest {

    static FilesProperties props(String secret) {
        return new FilesProperties(secret, false, null, null, null, null, null, null, null);
    }

    final FileRefSigner signer = new FileRefSigner(props("secret-of-at-least-32-bytes-long!!"), new ObjectMapper());
    final FileRefSigner.Payload payload = new FileRefSigner.Payload("f1", UUID.randomUUID(), ArtifactPurpose.MATERIAL,
            "application/pdf", 10, "de.pdf", "abc", 2_000_000_000L);

    @Test
    void verifiesItsOwnSignature() {
        assertThat(signer.verify(signer.sign(payload))).contains(payload);
    }

    @Test
    void rejectsTamperedOrForeignReferences() {
        String value = signer.sign(payload).value();
        String tampered = value.substring(0, 5) + (value.charAt(5) == 'A' ? 'B' : 'A') + value.substring(6);
        assertThat(signer.verify(new FileRef(tampered))).isEmpty();
        assertThat(signer.verify(new FileRef("khong-hop-le"))).isEmpty();
        FileRefSigner other = new FileRefSigner(props("another-secret-of-at-least-32-bytes"), new ObjectMapper());
        assertThat(other.verify(signer.sign(payload))).isEmpty();
    }
}
