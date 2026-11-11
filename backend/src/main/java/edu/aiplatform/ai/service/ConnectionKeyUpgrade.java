package edu.aiplatform.ai.service;

import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mã hóa lại khóa API lưu trước khi gắn AAD (2026-11-11) để mọi bản mã gắn với kết nối của nó. Lúc dùng khóa chỉ
 * chấp nhận bản mã đã gắn, nên khóa cũ chưa nâng cấp sẽ báo lỗi tới khi bước này chạy. Chạy lại không sao: dòng đã gắn
 * được bỏ qua; backend và worker cùng chạy thì chỉ một bên ghi được nhờ điều kiện trên bản mã cũ.
 */
@Component
class ConnectionKeyUpgrade {

    private static final Logger log = LoggerFactory.getLogger(ConnectionKeyUpgrade.class);

    private final AiConnectionRepository connectionRepository;
    private final KeyCipher keyCipher;

    ConnectionKeyUpgrade(AiConnectionRepository connectionRepository, KeyCipher keyCipher) {
        this.connectionRepository = connectionRepository;
        this.keyCipher = keyCipher;
    }

    @EventListener(ApplicationReadyEvent.class)
    void upgradeLegacyKeys() {
        if (!keyCipher.configured()) {
            return;
        }
        int upgraded = 0;
        int unreadable = 0;
        try {
            for (AiConnection connection : connectionRepository.listWithKey()) {
                switch (upgradeKey(connection)) {
                    case UPGRADED -> upgraded++;
                    case UNREADABLE -> unreadable++;
                    case BOUND -> { }
                }
            }
        } catch (RuntimeException failure) {
            log.warn("AI key upgrade stopped errorClass={}; it resumes on next start", failure.getClass().getName());
            return;
        }
        if (upgraded > 0 || unreadable > 0) {
            log.info("AI key upgrade finished upgraded={} unreadable={}", upgraded, unreadable);
        }
    }

    Outcome upgradeKey(AiConnection connection) {
        if (isBound(connection)) {
            return Outcome.BOUND;
        }
        String plain;
        try {
            plain = keyCipher.decrypt(connection.encryptedKey());
        } catch (RuntimeException failure) {
            // Khóa mã hóa đã đổi hoặc bản mã hỏng: người dùng phải nhập lại khóa, không ghi gì.
            return Outcome.UNREADABLE;
        }
        connectionRepository.replaceKeyCiphertext(connection.id(), connection.keyCiphertext(),
                keyCipher.encryptConnectionKey(connection.id(), connection.scope(), connection.ownerId(),
                        connection.provider(), plain));
        return Outcome.UPGRADED;
    }

    private boolean isBound(AiConnection connection) {
        try {
            keyCipher.decryptConnectionKey(connection);
            return true;
        } catch (RuntimeException failure) {
            return false;
        }
    }

    enum Outcome {
        BOUND, UPGRADED, UNREADABLE
    }
}
