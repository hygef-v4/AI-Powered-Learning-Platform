package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FilesPropertiesTest {

    private static FilesProperties props(String key, String sharedDrive, String clientId, String secret, String token) {
        return new FilesProperties("secret", false, null, null, key, sharedDrive, clientId, secret, token);
    }

    @Test
    void personalDriveNeedsClientAndRefreshToken() {
        FilesProperties personal = props(null, null, "client", "secret", "token");
        assertThat(personal.personalDriveConfigured()).isTrue();
        assertThat(personal.sharedDriveConfigured()).isFalse();
        assertThat(personal.driveConfigured()).isTrue();
        assertThat(props(null, null, "client", "secret", " ").driveConfigured()).isFalse();
    }

    @Test
    void sharedDriveWinsWhenBothAreConfigured() {
        FilesProperties both = props("key", "drive", "client", "secret", "token");
        assertThat(both.sharedDriveConfigured()).isTrue();
        assertThat(both.personalDriveConfigured()).isFalse();
        assertThat(props("key", null, null, null, null).driveConfigured()).isFalse();
    }
}
