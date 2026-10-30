package edu.aiplatform.identity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.model.OtpPurpose;
import edu.aiplatform.identity.integration.OtpChallengeStore.Issue;
import edu.aiplatform.identity.integration.OtpChallengeStore.Verification;
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
    void jobRerunResendsItsOwnCodeButNeverRevivesAUsedOrReplacedOne() {
        assertThat(challenges.issue(accountId, OtpPurpose.ACTIVATION, "048213", "job-1")).isEqualTo(Issue.NEW);
        assertThat(challenges.issue(accountId, OtpPurpose.ACTIVATION, "048213", "job-1")).as("job chạy lại").isEqualTo(Issue.RESEND);

        assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "048213")).isEqualTo(Verification.VALID);
        assertThat(challenges.issue(accountId, OtpPurpose.ACTIVATION, "048213", "job-1")).as("mã đã dùng").isEqualTo(Issue.STALE);
        assertThat(challenges.verify(accountId, OtpPurpose.ACTIVATION, "048213")).as("không hồi sinh mã").isEqualTo(Verification.INVALID);

        assertThat(challenges.issue(accountId, OtpPurpose.ACTIVATION, "111111", "job-2")).isEqualTo(Issue.NEW);
        assertThat(challenges.issue(accountId, OtpPurpose.ACTIVATION, "111111", "job-3")).isEqualTo(Issue.NEW);
        assertThat(challenges.issue(accountId, OtpPurpose.ACTIVATION, "111111", "job-2")).as("đã có mã mới hơn").isEqualTo(Issue.STALE);
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

        assertThat(tickets.take(ticket, OtpPurpose.ACTIVATION, accountId)).isFalse();
        assertThat(tickets.take(ticket, OtpPurpose.PASSWORD_RESET, accountId)).isFalse();
    }

    @Test
    void ticketCanBeTakenOnlyOnce() {
        String ticket = tickets.issue(accountId, OtpPurpose.PASSWORD_RESET);

        assertThat(tickets.take(ticket, OtpPurpose.PASSWORD_RESET, accountId)).isTrue();
        assertThat(tickets.take(ticket, OtpPurpose.PASSWORD_RESET, accountId)).isFalse();
        assertThat(tickets.peek(ticket, OtpPurpose.PASSWORD_RESET)).isEmpty();
        assertThat(redis.values).isEmpty();
    }

    @Test
    void newTicketReplacesOldAndRevokeRemovesPending() {
        String first = tickets.issue(accountId, OtpPurpose.PASSWORD_RESET);
        String second = tickets.issue(accountId, OtpPurpose.PASSWORD_RESET);

        assertThat(tickets.peek(first, OtpPurpose.PASSWORD_RESET)).isEmpty();
        assertThat(tickets.peek(second, OtpPurpose.PASSWORD_RESET)).contains(accountId);

        tickets.revoke(accountId, OtpPurpose.PASSWORD_RESET);
        assertThat(tickets.peek(second, OtpPurpose.PASSWORD_RESET)).isEmpty();
        assertThat(redis.values).isEmpty();
    }
}
