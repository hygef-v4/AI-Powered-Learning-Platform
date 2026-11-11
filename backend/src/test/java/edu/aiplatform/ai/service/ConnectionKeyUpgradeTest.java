package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.EncryptedKey;
import edu.aiplatform.ai.model.AiConnection.Scope;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConnectionKeyUpgradeTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID OWNER = UUID.randomUUID();

    private final AiConnectionRepository connectionRepository = mock(AiConnectionRepository.class);
    private final KeyCipher keyCipher = new KeyCipher(
            KeyCipherAndCatalogTest.properties(KeyCipherAndCatalogTest.KEY_V1, KeyCipherAndCatalogTest.KEY_V2));
    private final ConnectionKeyUpgrade upgrade = new ConnectionKeyUpgrade(connectionRepository, keyCipher);

    @Test
    void legacyKeyIsResealedForItsConnectionWithTheLatestKeyVersion() {
        EncryptedKey legacy = new KeyCipher(KeyCipherAndCatalogTest.properties(KeyCipherAndCatalogTest.KEY_V1, null))
                .encrypt("AIza-legacy-0000");
        when(connectionRepository.listWithKey()).thenReturn(List.of(connection(legacy)));

        upgrade.upgradeLegacyKeys();

        ArgumentCaptor<EncryptedKey> resealed = ArgumentCaptor.forClass(EncryptedKey.class);
        verify(connectionRepository).replaceKeyCiphertext(eq(ID), eq(legacy.ciphertext()), resealed.capture());
        assertThat(resealed.getValue().version()).isEqualTo(2);
        assertThat(keyCipher.decryptConnectionKey(connection(resealed.getValue()))).isEqualTo("AIza-legacy-0000");
    }

    @Test
    void boundAndUnreadableKeysAreLeftAlone() {
        AiConnection bound = connection(
                keyCipher.encryptConnectionKey(ID, Scope.PERSONAL, OWNER, AiProvider.GEMINI, "AIza-bound-1111"));
        AiConnection unreadable = connection(new EncryptedKey(new byte[] {1, 2, 3}, new byte[12], 1));

        assertThat(upgrade.upgradeKey(bound)).isEqualTo(ConnectionKeyUpgrade.Outcome.BOUND);
        assertThat(upgrade.upgradeKey(unreadable)).isEqualTo(ConnectionKeyUpgrade.Outcome.UNREADABLE);
        verify(connectionRepository, never()).replaceKeyCiphertext(any(), any(), any());
    }

    private static AiConnection connection(EncryptedKey key) {
        return AiConnection.create(ID, Scope.PERSONAL, OWNER, AiProvider.GEMINI, "gemini-2.5-flash", key, "0000",
                Instant.EPOCH);
    }
}
