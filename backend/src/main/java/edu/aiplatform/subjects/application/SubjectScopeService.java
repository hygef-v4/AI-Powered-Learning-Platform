package edu.aiplatform.subjects.application;

import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * U04 cài contract U01: chỉ kiểm phân công hiện thời, không cache và không gọi ngược IAuthorization.
 * U01 vẫn phải kiểm session, role và trạng thái ACTIVE của account.
 * Môn ARCHIVED vẫn có scope để xem lịch sử; unit ghi tài nguyên kiểm isActive riêng.
 */
@Service
public class SubjectScopeService implements ISubjectScope {

    private final ISubjectRepository repository;

    public SubjectScopeService(ISubjectRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isSubjectManager(UUID accountId, UUID subjectId) {
        return accountId != null && subjectId != null && repository.isManagedBy(accountId, subjectId);
    }

    @Override
    public List<UUID> listManagedSubjects(UUID accountId) {
        return accountId == null ? List.of() : repository.findManagedBy(accountId).stream().map(Subject::id).toList();
    }
}
