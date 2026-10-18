package edu.aiplatform.subjects.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SubjectTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID MANAGER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED = Instant.parse("2026-10-18T00:00:00Z");
    private static final Instant LATER = CREATED.plusSeconds(60);

    private Subject subject() {
        return Subject.create(ID, "swr302", "  Software Requirements  ", null, null, CREATED);
    }

    @Test
    void createsActiveSubjectWithoutManagerWithPublicVersionOne() {
        Subject subject = subject();
        assertThat(subject.code().value()).isEqualTo("SWR302");
        assertThat(subject.name()).isEqualTo("Software Requirements");
        assertThat(subject.status()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(subject.managerId()).isNull();
        assertThat(subject.version()).isEqualTo(1);
        assertThat(subject.createdAt()).isEqualTo(subject.updatedAt()).isEqualTo(CREATED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n"})
    void rejectsMissingName(String name) {
        assertThatThrownBy(() -> subject().updateInformation(name, null, LATER))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUBJECT_NAME_INVALID");
    }

    @Test
    void enforcesNameAndDescriptionBoundariesOnCreateAndEdit() {
        Subject boundary = Subject.create(ID, "SWR302", "N".repeat(200), "D".repeat(2000), MANAGER, CREATED);
        assertThat(boundary.description()).hasSize(2000);
        assertThatThrownBy(() -> Subject.create(ID, "SWR302", "N".repeat(201), null, null, CREATED))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUBJECT_NAME_INVALID");
        assertThatThrownBy(() -> subject().updateInformation("Name", "D".repeat(2001), LATER))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUBJECT_DESCRIPTION_INVALID");
        assertThat(subject().description()).isNull();
    }

    @Test
    void editsInformationWithoutChangingIdentityCodeOrOriginalSnapshot() {
        Subject original = subject();
        Subject edited = original.updateInformation("  New name ", "Description", LATER);
        assertThat(edited.id()).isEqualTo(original.id());
        assertThat(edited.code()).isEqualTo(original.code());
        assertThat(edited.createdAt()).isEqualTo(CREATED);
        assertThat(edited.updatedAt()).isEqualTo(LATER);
        assertThat(edited.version()).isEqualTo(2);
        assertThat(original.name()).isEqualTo("Software Requirements");
        assertThat(original.version()).isEqualTo(1);
    }

    @Test
    void changesAndClearsManager() {
        Subject assigned = subject().changeManager(MANAGER, LATER);
        Subject cleared = assigned.changeManager(null, LATER);
        assertThat(cleared.managerId()).isNull();
        assertThat(cleared.version()).isEqualTo(assigned.version() + 1);
        assertThat(assigned.managerId()).isEqualTo(MANAGER);
    }

    @Test
    void archiveUnassignsManagerInOneVersionAndReopenDoesNotReassign() {
        Subject assigned = subject().changeManager(MANAGER, LATER);
        Subject archived = assigned.archive(LATER);
        Subject reopened = archived.reopen(LATER);
        assertThat(archived.status()).isEqualTo(SubjectStatus.ARCHIVED);
        assertThat(archived.managerId()).isNull();
        assertThat(archived.version()).isEqualTo(assigned.version() + 1);
        assertThat(reopened.status()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(reopened.managerId()).isNull();
        assertThat(reopened.name()).isEqualTo(assigned.name());
        assertThat(reopened.version()).isEqualTo(assigned.version() + 2);
    }

    @Test
    void validNoOpsKeepTheSameSnapshotButStaleVersionIsStillRejected() {
        Subject subject = subject();
        subject.requireVersion(1);
        assertThat(subject.changeManager(null, LATER)).isSameAs(subject);
        assertThat(subject.reopen(LATER)).isSameAs(subject);
        assertThat(subject.updateInformation(" Software Requirements ", null, LATER)).isSameAs(subject);
        Subject archived = subject.archive(LATER);
        assertThat(archived.archive(LATER)).isSameAs(archived);
        assertThatThrownBy(() -> archived.requireVersion(1)).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("SUBJECT_VERSION_CONFLICT");
    }
}
