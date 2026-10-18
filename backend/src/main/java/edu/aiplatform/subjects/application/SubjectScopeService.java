package edu.aiplatform.subjects.application;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * U04 cài contract U01: chỉ kiểm phân công hiện thời, không cache và không gọi ngược IAuthorization.
 * U01 vẫn phải kiểm session, role và trạng thái ACTIVE của account.
 * Vô hiệu hóa tài khoản thì U01 gọi {@link #releaseManagerAssignments} để gỡ khỏi mọi môn (BR-U04-12).
 */
@Service
public class SubjectScopeService implements ISubjectScope {

    private final ISubjectRepository repository;
    private final SubjectAuditRecorder audit;
    private final Clock clock;

    public SubjectScopeService(ISubjectRepository repository, SubjectAuditRecorder audit, Clock clock) {
        this.repository = repository;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public boolean isSubjectManager(UUID accountId, UUID subjectId) {
        return accountId != null && subjectId != null && repository.isManagedBy(accountId, subjectId);
    }

    @Override
    public List<UUID> listManagedSubjects(UUID accountId) {
        return accountId == null ? List.of() : repository.findManagedBy(accountId).stream().map(Subject::id).toList();
    }

    /** Khóa từng môn theo thứ tự id, gỡ Chủ nhiệm, tăng version, ghi audit trong transaction của U01. */
    @Override
    @Transactional
    public List<UUID> releaseManagerAssignments(UUID accountId, Actor actor) {
        Objects.requireNonNull(actor, "actor");
        if (accountId == null) {
            return List.of();
        }
        List<UUID> ids = repository.findManagedBy(accountId).stream().map(Subject::id).sorted().toList();
        Instant now = clock.instant();
        List<UUID> released = new ArrayList<>();
        for (UUID id : ids) {
            Subject before = repository.findByIdForUpdate(id).orElse(null);
            if (before == null || !accountId.equals(before.managerId())) {
                continue;
            }
            Subject after = before.changeManager(null, now);
            if (!repository.update(after, before.version())) {
                throw ApiException.conflict("SUBJECT_VERSION_CONFLICT", "Dữ liệu môn đã thay đổi. Hãy thử lại");
            }
            audit.managerChanged(actor, before, after);
            released.add(id);
        }
        return List.copyOf(released);
    }
}
