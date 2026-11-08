package edu.aiplatform.academics.contract.stub;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.shared.stub.Stubs;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của {@code IClassAccess} (U05): lớp mẫu đang mở, mọi kiểm tra quyền trả {@code true}, danh sách rỗng. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class AcademicsContractStubs {

    /** Provides the fallback class access contract when no implementation is configured. */
    @Bean
    @ConditionalOnMissingBean
    IClassAccess classAccessStub() {
        return Stubs.announce(IClassAccess.class, new IClassAccess() {
            /** Loads the lightweight class reference for a class ID. */
            @Override
            public Optional<ClassRef> getClassRef(UUID classId) {
                return Optional.of(new ClassRef(classId, Stubs.derivedId("subject", classId), "MAU",
                        "Lớp mẫu", ClassStatus.OPEN, null, false));
            }

            /** Checks whether the account is an active student in the class. */
            @Override
            public boolean isActiveStudent(UUID accountId, UUID classId) {
                return true;
            }

            /** Lists active student account IDs in the class. */
            @Override
            public List<UUID> listActiveStudents(UUID classId) {
                return List.of();
            }

            /** Checks whether the account manages the class subject. */
            @Override
            public boolean isClassManager(UUID accountId, UUID classId) {
                return true;
            }

            /** Lists open classes associated with an account. */
            @Override
            public List<ClassRef> listOpenClassesOf(UUID accountId) {
                return List.of();
            }

            /** Lists open classes belonging to a subject. */
            @Override
            public List<ClassRef> listOpenClassesOfSubject(UUID subjectId) {
                return List.of();
            }

            /** Counts classes grouped by lifecycle status. */
            @Override
            public Map<ClassStatus, Long> countClassesByStatus() {
                return Map.of();
            }

            /** Counts active enrollments across the platform. */
            @Override
            public long countActiveEnrollments() {
                return 0;
            }
        });
    }
}
