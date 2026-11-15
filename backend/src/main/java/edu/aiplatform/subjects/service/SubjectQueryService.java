package edu.aiplatform.subjects.service;

import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Public query của U04 cho các unit khác; quyền của hành động được kiểm ở unit gọi. */
@Service
public class SubjectQueryService implements ISubjectQuery {

    private final ISubjectRepository subjectRepository;

    public SubjectQueryService(ISubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Override
    public Optional<SubjectRef> getSubject(UUID subjectId) {
        return subjectId == null
                ? Optional.empty()
                : subjectRepository.findById(subjectId).map(SubjectQueryService::mapToSubjectRef);
    }

    @Override
    public boolean isActive(UUID subjectId) {
        return getSubject(subjectId)
                .filter(subject -> subject.status() == SubjectStatus.ACTIVE)
                .isPresent();
    }

    @Override
    public List<SubjectRef> listManagedSubjects(UUID accountId) {
        return accountId == null
                ? List.of()
                : subjectRepository.findSubjectsManagedBy(accountId).stream()
                        .map(SubjectQueryService::mapToSubjectRef)
                        .toList();
    }

    @Override
    public Map<SubjectStatus, Long> countSubjectsByStatus() {
        return subjectRepository.countSubjectsByStatus(null);
    }

    private static SubjectRef mapToSubjectRef(Subject subject) {
        return new SubjectRef(
                subject.id(),
                subject.code().value(),
                subject.name(),
                subject.status(),
                subject.managerId());
    }
}
