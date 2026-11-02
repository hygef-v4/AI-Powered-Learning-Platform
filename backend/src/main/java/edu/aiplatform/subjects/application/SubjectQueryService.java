package edu.aiplatform.subjects.application;

import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Public query của U04 cho các unit khác; quyền của hành động được kiểm ở unit gọi. */
@Service
public class SubjectQueryService implements ISubjectQuery {

    private final ISubjectRepository repository;

    public SubjectQueryService(ISubjectRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SubjectRef> getSubject(UUID subjectId) {
        return subjectId == null ? Optional.empty() : repository.findById(subjectId).map(SubjectQueryService::reference);
    }

    @Override
    public boolean isActive(UUID subjectId) {
        return getSubject(subjectId).filter(subject -> subject.status() == SubjectStatus.ACTIVE).isPresent();
    }

    @Override
    public List<SubjectRef> listManagedSubjects(UUID accountId) {
        return accountId == null ? List.of() : repository.findManagedBy(accountId).stream()
                .map(SubjectQueryService::reference).toList();
    }

    @Override
    public Map<SubjectStatus, Long> countSubjectsByStatus() {
        return repository.countByStatus(null);
    }

    private static SubjectRef reference(Subject subject) {
        return new SubjectRef(subject.id(), subject.code().value(), subject.name(), subject.status(), subject.managerId());
    }
}
