package edu.aiplatform.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.domain.OtpPurpose;
import edu.aiplatform.identity.infrastructure.OtpChallengeStore.Verification;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpStoresTest {

    private final UUID accountId = UUID.randomUUID();
    private FakeRedis redis;
    private OtpChallengeStore challenges;
    private VerificationTicketStore tickets;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        challenges = new OtpChallengeStore(redis.template, IdentityTestData.properties());
        tickets = new VerificationTicketStore(redis.template, IdentityTestData.properties());
    }

    @Test
    void storesOnlyHashWithTenMinuteExpiry() {
        challenges.replace(accountId, OtpPurpose.ACTIVATION, "048213");

        String key = OtpChallengeStore.key(accountId, OtpPurpose.ACTIVATION);
        assertThat(redis.hashes.get(key)).doesNotContainValue("048213").containsEntry("attemptsLeft", "5");
        org.mockito.Mockito.verify(redis.template).expire(key, Duration.ofMinutes(10));
    }

    @Test
    void correctCodeWorksOnce() {
        challenges.replace(accountId, OtpPurpose.ACTIVATION, "048213");

        assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "048213")).isEqualTo(Verification.VALID);
        assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "048213")).isEqualTo(Verification.INVALID);
    }

    @Test
    void fiveWrongGuessesDestroyTheCode() {
        challenges.replace(accountId, OtpPurpose.ACTIVATION, "048213");
        for (int i = 0; i < 5; i++) {
            assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "111111")).isEqualTo(Verification.INVALID);
        }
        assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "048213")).isEqualTo(Verification.INVALID);
    }

    @Test
    void newCodeReplacesOldCode() {
        challenges.replace(accountId, OtpPurpose.ACTIVATION, "111111");
        challenges.replace(accountId, OtpPurpose.ACTIVATION, "222222");

        assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "111111")).isEqualTo(Verification.INVALID);
        assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "222222")).isEqualTo(Verification.VALID);
    }

    @Test
    void codeOfOnePurposeDoesNotWorkForTheOther() {
        challenges.replace(accountId, OtpPurpose.ACTIVATION, "048213");
        assertThat(challenges.verify(accountId, OtpPurpose.PASSWORD_RESET, "048213")).isEqualTo(Verification.INVALID);
    }

    @Test
    void deleteAllRemovesEveryPurpose() {
        challenges.replace(accountId, OtpPurpose.ACTIVATION, "048213");
        challenges.replace(accountId, OtpPurpose.PASSWORD_RESET, "048213");

        challenges.deleteAll(accountId);

        assertThat(redis.hashes).isEmpty();
    }

    @Test
    void ticketIsBoundToPurposeAndStoredHashed() {
        String ticket = tickets.issue(accountId, OtpPurpose.PASSWORD_RESET);

        assertThat(redis.values.keySet()).noneMatch(key -> key.contains(ticket));
        assertThat(tickets.peek(ticket, OtpPurpose.PASSWORD_RESET)).contains(accountId);
        assertThat(tickets.peek(ticket, OtpPurpose.ACTIVATION)).isEmpty();
        assertThat(tickets.peek(ticket, OtpPurpose.PASSWORD_RESET)).contains(accountId);

        tickets.consume(ticket);
        assertThat(tickets.peek(ticket, OtpPurpose.PASSWORD_RESET)).isEmpty();
    }
}
