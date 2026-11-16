package edu.aiplatform.files.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FilesPropertiesTest {

    private static FilesProperties props(String key, String sharedDrive, String clientId, String secret, String token) {
        return new FilesProperties("secret", false, null, null, key, sharedDrive, clientId, secret, token, null, null);
    }

    @Test
    void personalDriveNeedsClientAndRefreshToken() {
        FilesProperties personal = props(null, null, "client", "secret", "token");
        assertThat(personal.hasPersonalDrive()).isTrue();
        assertThat(personal.hasSharedDrive()).isFalse();
        assertThat(personal.hasDrive()).isTrue();
        assertThat(props(null, null, "client", "secret", " ").hasDrive()).isFalse();
    }

    @Test
    void sharedDriveWinsWhenBothAreConfigured() {
        FilesProperties both = props("key", "drive", "client", "secret", "token");
        assertThat(both.hasSharedDrive()).isTrue();
        assertThat(both.hasPersonalDrive()).isFalse();
        assertThat(props("key", null, null, null, null).hasDrive()).isFalse();
    }
}
