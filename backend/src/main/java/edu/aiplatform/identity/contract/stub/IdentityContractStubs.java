package edu.aiplatform.identity.contract.stub;

import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.IMail;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.stub.Stubs;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của interface U01; U01 (và U04, U05 với phạm vi) cài thật thì bản tạm tự tắt. Quyền luôn cho phép. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityContractStubs {

    @Bean
    @ConditionalOnMissingBean
    IAuthorization authorizationStub() {
        return Stubs.announce(IAuthorization.class, (actor, action, resourceRef) -> AuthorizationDecision.allow());
    }

    @Bean
    @ConditionalOnMissingBean
    IAccountLookup accountLookupStub() {
        return Stubs.announce(IAccountLookup.class, new IAccountLookup() {
            @Override
            public List<AccountContact> findStudents(String query, int limit) {
                return List.of();
            }

            @Override
            public List<AccountContact> findAccounts(String query, Set<Role> roles, int limit) {
                return List.of();
            }

            @Override
            public List<AccountContact> findByEmails(Collection<String> emails) {
                return List.of();
            }

            @Override
            public Optional<AccountContact> getContact(UUID accountId) {
                String email = "user-" + accountId.toString().substring(0, 8) + "@example.com";
                return Optional.of(new AccountContact(accountId, email, "Người dùng mẫu", Role.STUDENT, AccountStatus.ACTIVE));
            }

            @Override
            public Map<Role, Map<AccountStatus, Long>> countByRoleAndStatus() {
                return Map.of();
            }
        });
    }

    /** Không gửi mail thật; chỉ ghi người nhận và tiêu đề, không ghi nội dung (có thể chứa OTP). */
    @Bean
    @ConditionalOnMissingBean
    IMail mailStub() {
        return Stubs.announce(IMail.class,
                message -> Stubs.called(IMail.class, "bỏ qua email tới " + message.to() + ": " + message.subject()));
    }

    @Bean
    @ConditionalOnMissingBean
    ISubjectScope subjectScopeStub() {
        return Stubs.announce(ISubjectScope.class, new ISubjectScope() {
            @Override
            public boolean isSubjectManager(UUID accountId, UUID subjectId) {
                return true;
            }

            @Override
            public List<UUID> listManagedSubjects(UUID accountId) {
                return List.of();
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IClassScope classScopeStub() {
        return Stubs.announce(IClassScope.class, new IClassScope() {
            @Override
            public boolean isTeacherOf(UUID accountId, UUID classId) {
                return true;
            }

            @Override
            public boolean isEnrolledStudent(UUID accountId, UUID classId) {
                return true;
            }

            @Override
            public Optional<UUID> subjectOfClass(UUID classId) {
                return Optional.of(Stubs.derivedId("subject", classId));
            }

            @Override
            public List<UUID> listTaughtClasses(UUID accountId) {
                return List.of();
            }
        });
    }
}
