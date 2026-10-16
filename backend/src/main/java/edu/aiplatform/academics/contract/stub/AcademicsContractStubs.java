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

    @Bean
    @ConditionalOnMissingBean
    IClassAccess classAccessStub() {
        return Stubs.announce(IClassAccess.class, new IClassAccess() {
            @Override
            public Optional<ClassRef> getClassRef(UUID classId) {
                return Optional.of(new ClassRef(classId, Stubs.derivedId("subject", classId), "MAU", "Lớp mẫu",
                        ClassStatus.OPEN, null, false));
            }

            @Override
            public boolean isActiveStudent(UUID accountId, UUID classId) {
                return true;
            }

            @Override
            public List<UUID> listActiveStudents(UUID classId) {
                return List.of();
            }

            @Override
            public boolean isClassManager(UUID accountId, UUID classId) {
                return true;
            }

            @Override
            public List<ClassRef> listOpenClassesOf(UUID accountId) {
                return List.of();
            }

            @Override
            public List<ClassRef> listOpenClassesOfSubject(UUID subjectId) {
                return List.of();
            }

            @Override
            public Map<ClassStatus, Long> countClassesByStatus() {
                return Map.of();
            }

            @Override
            public long countActiveEnrollments() {
                return 0;
            }
        });
    }
}
