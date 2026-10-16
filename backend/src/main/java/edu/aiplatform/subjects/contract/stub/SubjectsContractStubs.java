package edu.aiplatform.subjects.contract.stub;

import edu.aiplatform.shared.stub.Stubs;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectClassSummary;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của interface U04 (và {@code ISubjectClasses} do U05 cài): môn mẫu đang mở, môn chưa có lớp nào. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class SubjectsContractStubs {

    @Bean
    @ConditionalOnMissingBean
    ISubjectQuery subjectQueryStub() {
        return Stubs.announce(ISubjectQuery.class, new ISubjectQuery() {
            @Override
            public Optional<SubjectRef> getSubject(UUID subjectId) {
                return Optional.of(new SubjectRef(subjectId, "MAU", "Môn mẫu", SubjectStatus.ACTIVE, null));
            }

            @Override
            public boolean isActive(UUID subjectId) {
                return true;
            }

            @Override
            public List<SubjectRef> listManagedSubjects(UUID accountId) {
                return List.of();
            }

            @Override
            public Map<SubjectStatus, Long> countSubjectsByStatus() {
                return Map.of();
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    ISubjectClasses subjectClassesStub() {
        return Stubs.announce(ISubjectClasses.class, new ISubjectClasses() {
            @Override
            public List<SubjectClassSummary> listClassesOfSubject(UUID subjectId) {
                return List.of();
            }

            @Override
            public Map<UUID, Integer> countClasses(Collection<UUID> subjectIds) {
                return subjectIds.stream().distinct().collect(Collectors.toMap(Function.identity(), id -> 0));
            }

            @Override
            public int countUnarchivedClasses(UUID subjectId) {
                return 0;
            }
        });
    }
}
