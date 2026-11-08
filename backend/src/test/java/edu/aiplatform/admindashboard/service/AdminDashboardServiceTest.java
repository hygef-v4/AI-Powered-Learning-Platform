package edu.aiplatform.admindashboard.service;


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
            when(f.accountLookup.countByRoleAndStatus()).thenReturn(Map.of(Role.STUDENT, Map.of(AccountStatus.ACTIVE, 3L), Role.TEACHER, Map.of(AccountStatus.PENDING, 2L)));
            when(f.subjectQuery.countSubjectsByStatus()).thenReturn(Map.of(SubjectStatus.ACTIVE, 4L));
            when(f.classAccess.countClassesByStatus()).thenReturn(Map.of(ClassStatus.OPEN, 2L));
            when(f.classAccess.countActiveEnrollments()).thenReturn(7L);
            var view = f.adminDashboardService.loadDashboard(f.admin);
            assertThat(view.accounts().data().total()).isEqualTo(5L);
            assertThat(view.accounts().data().activeTotal()).isEqualTo(3L);
            assertThat(view.accounts().data().byRoleAndStatus().get(Role.ADMIN).get(AccountStatus.ACTIVE)).isZero();
            assertThat(view.subjects().data()).containsEntry(SubjectStatus.ARCHIVED, 0L);
            assertThat(view.classes().data()).containsEntry(ClassStatus.OPEN, 2L);
            assertThat(view.activeEnrollments().data()).isEqualTo(7L);
            assertThat(view.calculatedAt()).isEqualTo(f.now);
            verify(f.accountLookup).countByRoleAndStatus();
        }
    }
    @Test void failuresNullNegativeAndOverflowAreIndependentUnavailableSections() {
        try (var f = new DashboardFixture()) {
            when(f.accountLookup.countByRoleAndStatus()).thenReturn(Map.of(Role.STUDENT, Map.of(AccountStatus.ACTIVE, Long.MAX_VALUE, AccountStatus.PENDING, 1L)));
            when(f.subjectQuery.countSubjectsByStatus()).thenThrow(new IllegalStateException("secret"));
            when(f.classAccess.countClassesByStatus()).thenReturn(Map.of(ClassStatus.OPEN, 2L));
            when(f.classAccess.countActiveEnrollments()).thenReturn(-1L);
            var view = f.adminDashboardService.loadDashboard(f.admin);
            assertThat(view.accounts().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(view.subjects().data()).isNull();
            assertThat(view.classes().data()).containsEntry(ClassStatus.OPEN, 2L);
            assertThat(view.activeEnrollments().availability()).isEqualTo(Availability.UNAVAILABLE);
            when(f.accountLookup.countByRoleAndStatus()).thenReturn(null);
            when(f.classAccess.countClassesByStatus()).thenReturn(Map.of(ClassStatus.OPEN, -1L));
            view = f.adminDashboardService.loadDashboard(f.admin);
            assertThat(view.accounts().data()).isNull();
            assertThat(view.classes().data()).isNull();
        }
    }
    @Test void successfulEmptyCountsAreAvailableZeroAndReloadOnEveryRead() {
        try (var f = new DashboardFixture()) {
            assertThat(f.adminDashboardService.loadDashboard(f.admin).accounts().data().total()).isZero();
            when(f.accountLookup.countByRoleAndStatus()).thenReturn(Map.of(Role.ADMIN, Map.of(AccountStatus.ACTIVE, 1L)));
            assertThat(f.adminDashboardService.loadDashboard(f.admin).accounts().data().total()).isEqualTo(1);
            verify(f.accountLookup, times(2)).countByRoleAndStatus();
        }
    }
}
