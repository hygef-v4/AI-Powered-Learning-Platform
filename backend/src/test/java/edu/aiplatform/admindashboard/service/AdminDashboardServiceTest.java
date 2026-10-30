package edu.aiplatform.admindashboard.service;

import edu.aiplatform.admindashboard.dto.AdminDashboardViews;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.admindashboard.dto.AdminDashboardViews.Availability;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdminDashboardServiceTest {
    @Test void sparseCountsHaveCompleteBreakdownAndCheckedTotals() {
        try (var f = new DashboardFixture()) {
            when(f.accounts.countByRoleAndStatus()).thenReturn(Map.of(Role.STUDENT, Map.of(AccountStatus.ACTIVE, 3L), Role.TEACHER, Map.of(AccountStatus.PENDING, 2L)));
            when(f.subjects.countSubjectsByStatus()).thenReturn(Map.of(SubjectStatus.ACTIVE, 4L));
            when(f.classes.countClassesByStatus()).thenReturn(Map.of(ClassStatus.OPEN, 2L));
            when(f.classes.countActiveEnrollments()).thenReturn(7L);
            var view = f.service.dashboard(f.admin);
            assertThat(view.accounts().data().total()).isEqualTo(5L);
            assertThat(view.accounts().data().activeTotal()).isEqualTo(3L);
            assertThat(view.accounts().data().byRoleAndStatus().get(Role.ADMIN).get(AccountStatus.ACTIVE)).isZero();
            assertThat(view.subjects().data()).containsEntry(SubjectStatus.ARCHIVED, 0L);
            assertThat(view.classes().data()).containsEntry(ClassStatus.OPEN, 2L);
            assertThat(view.activeEnrollments().data()).isEqualTo(7L);
            assertThat(view.calculatedAt()).isEqualTo(f.now);
            verify(f.accounts).countByRoleAndStatus();
        }
    }
    @Test void failuresNullNegativeAndOverflowAreIndependentUnavailableSections() {
        try (var f = new DashboardFixture()) {
            when(f.accounts.countByRoleAndStatus()).thenReturn(Map.of(Role.STUDENT, Map.of(AccountStatus.ACTIVE, Long.MAX_VALUE, AccountStatus.PENDING, 1L)));
            when(f.subjects.countSubjectsByStatus()).thenThrow(new IllegalStateException("secret"));
            when(f.classes.countClassesByStatus()).thenReturn(Map.of(ClassStatus.OPEN, 2L));
            when(f.classes.countActiveEnrollments()).thenReturn(-1L);
            var view = f.service.dashboard(f.admin);
            assertThat(view.accounts().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(view.subjects().data()).isNull();
            assertThat(view.classes().data()).containsEntry(ClassStatus.OPEN, 2L);
            assertThat(view.activeEnrollments().availability()).isEqualTo(Availability.UNAVAILABLE);
            when(f.accounts.countByRoleAndStatus()).thenReturn(null);
            when(f.classes.countClassesByStatus()).thenReturn(Map.of(ClassStatus.OPEN, -1L));
            view = f.service.dashboard(f.admin);
            assertThat(view.accounts().data()).isNull();
            assertThat(view.classes().data()).isNull();
        }
    }
    @Test void successfulEmptyCountsAreAvailableZeroAndReloadOnEveryRead() {
        try (var f = new DashboardFixture()) {
            assertThat(f.service.dashboard(f.admin).accounts().data().total()).isZero();
            when(f.accounts.countByRoleAndStatus()).thenReturn(Map.of(Role.ADMIN, Map.of(AccountStatus.ACTIVE, 1L)));
            assertThat(f.service.dashboard(f.admin).accounts().data().total()).isEqualTo(1);
            verify(f.accounts, times(2)).countByRoleAndStatus();
        }
    }
}
