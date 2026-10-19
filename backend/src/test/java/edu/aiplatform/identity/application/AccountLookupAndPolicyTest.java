package edu.aiplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.contract.ScopeKind;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountLookupAndPolicyTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountLookupService lookup = new AccountLookupService(accounts);

    @Test
    void studentsAreActiveOnlyAndAtMostTwenty() {
        Account student = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "hash");
        when(accounts.findForLookup("tai", Set.of(Role.STUDENT), AccountStatus.ACTIVE, 20)).thenReturn(List.of(student));

        List<AccountContact> found = lookup.findStudents("tai", 500);

        assertThat(found).singleElement().satisfies(c -> {
            assertThat(c.email()).isEqualTo("taivm@fpt.edu.vn");
            assertThat(c.status()).isEqualTo(AccountStatus.ACTIVE);
        });
    }

    @Test
    void emptyRolesFindNothing() {
        lookup.findAccounts("a", null, 5);
        verify(accounts).findForLookup(eq("a"), eq(Set.of()), eq(null), anyInt());
    }

    @Test
    void emailsAreNormalizedBeforeLookup() {
        lookup.findByEmails(List.of(" TaiVM@FPT.edu.vn", "taivm@fpt.edu.vn", " "));
        verify(accounts).findByEmails(List.of("taivm@fpt.edu.vn"));

        assertThat(lookup.findByEmails(List.of())).isEmpty();
        verify(accounts, never()).findForLookup(anyString(), any(), any(), anyInt());
    }

    /** Mọi action khai báo cho unit khác phải có dòng trong bảng quyền, nếu không sẽ bị từ chối mặc định (Q14). */
    @Test
    void everyDeclaredActionHasAPolicyRule() throws IllegalAccessException {
        for (Field field : Actions.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                String action = (String) field.get(null);
                assertThat(AccessPolicy.ruleFor(action)).as(action).isPresent();
            }
        }
    }

    @Test
    void adminActionsAreAdminOnlyAndUploadExcludesAdmin() {
        Arrays.stream(Actions.class.getDeclaredFields())
                .filter(f -> f.getName().startsWith("ACCOUNT_") || f.getName().startsWith("SETTINGS_")
                        || f.getName().startsWith("SUBJECT_")
                        || f.getName().equals("AUDIT_READ"))
                .forEach(f -> assertThat(AccessPolicy.ruleFor(readName(f)).orElseThrow().roles())
                        .containsExactly(Role.ADMIN));
        assertThat(AccessPolicy.ruleFor(Actions.FILE_UPLOAD).orElseThrow().roles()).doesNotContain(Role.ADMIN);
    }

    /** U05: Admin không có màn lớp; xem lớp theo phạm vi lớp (R3/R5), quản lý lớp theo phạm vi môn (R2). */
    @Test
    void classActionsExcludeAdminAndUseClassOrSubjectScope() {
        Arrays.stream(Actions.class.getDeclaredFields())
                .filter(f -> f.getName().startsWith("CLASS_"))
                .forEach(f -> assertThat(AccessPolicy.ruleFor(readName(f)).orElseThrow().roles()).doesNotContain(Role.ADMIN));
        assertThat(AccessPolicy.ruleFor(Actions.CLASS_VIEW).orElseThrow().scope()).isEqualTo(ScopeKind.CLASS);
        var manage = AccessPolicy.ruleFor(Actions.CLASS_MANAGE).orElseThrow();
        assertThat(manage.scope()).isEqualTo(ScopeKind.SUBJECT);
        assertThat(manage.roles()).containsExactly(Role.SUBJECT_MANAGER);
    }

    /** U10: ngân hàng câu hỏi chỉ cho Chủ nhiệm môn của môn (R2, BR-U10-01). */
    @Test
    void questionBankActionsAreSubjectManagerOnlyWithSubjectScope() {
        for (String action : new String[] {Actions.QUESTION_BANK_VIEW, Actions.QUESTION_BANK_MANAGE}) {
            var rule = AccessPolicy.ruleFor(action).orElseThrow();
            assertThat(rule.roles()).containsExactly(Role.SUBJECT_MANAGER);
            assertThat(rule.scope()).isEqualTo(ScopeKind.SUBJECT);
        }
    }

    /** U10: Quiz của môn theo phạm vi môn (R2), Quiz của lớp theo giảng viên lớp (R3/R4); Admin, Student không soạn. */
    @Test
    void quizActionsUseSubjectOrClassScope() {
        var subjectQuiz = AccessPolicy.ruleFor(Actions.QUIZ_SUBJECT_MANAGE).orElseThrow();
        assertThat(subjectQuiz.roles()).containsExactly(Role.SUBJECT_MANAGER);
        assertThat(subjectQuiz.scope()).isEqualTo(ScopeKind.SUBJECT);
        var classQuiz = AccessPolicy.ruleFor(Actions.QUIZ_CLASS_MANAGE).orElseThrow();
        assertThat(classQuiz.roles()).containsExactlyInAnyOrder(Role.TEACHER, Role.SUBJECT_MANAGER);
        assertThat(classQuiz.scope()).isEqualTo(ScopeKind.CLASS);
    }

    /** U11: chỉ sinh viên đang học lớp làm Quiz Practice (R5, BR-U11-01). */
    @Test
    void quizPracticeIsStudentOnlyWithClassScope() {
        var practice = AccessPolicy.ruleFor(Actions.QUIZ_PRACTICE).orElseThrow();
        assertThat(practice.roles()).containsExactly(Role.STUDENT);
        assertThat(practice.scope()).isEqualTo(ScopeKind.CLASS);
    }

    /** U12: nhóm do giảng viên được giao dạy quản lý; sinh viên đang học làm bài nhóm. */
    @Test
    void groupActionsUseClassScope() {
        var manage = AccessPolicy.ruleFor(Actions.GROUP_MANAGE).orElseThrow();
        assertThat(manage.scope()).isEqualTo(ScopeKind.CLASS);
        assertThat(manage.roles()).containsExactlyInAnyOrder(Role.TEACHER, Role.SUBJECT_MANAGER);
        var work = AccessPolicy.ruleFor(Actions.GROUP_WORK).orElseThrow();
        assertThat(work.scope()).isEqualTo(ScopeKind.CLASS);
        assertThat(work.roles()).containsExactly(Role.STUDENT);
    }

    private static String readName(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
