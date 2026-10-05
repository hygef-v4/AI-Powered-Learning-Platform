package edu.aiplatform.academics.application;

import edu.aiplatform.academics.port.*;
import edu.aiplatform.identity.port.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U04: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U04: Bước 10.
 */
@Service
public class ScopeQueryService implements SubjectScopePort, ClassScopePort, ClassAccessPort {

    @Override
    public boolean isSubjectManager(UUID accountId, UUID subjectId) {
        return false;
    }

    @Override
    public List<UUID> listManagedSubjects(UUID accountId) {
        return List.of();
    }

    @Override
    public boolean isTeacherOf(UUID accountId, UUID classId) {
        return false;
    }

    @Override
    public UUID subjectOfClass(UUID classId) {
        throw new UnsupportedOperationException("Chưa cài: U04");
    }

    @Override
    public List<UUID> listTaughtClasses(UUID accountId) {
        return List.of();
    }

    @Override
    public Optional<ClassRef> getClassRef(UUID classId) {
        return Optional.empty();
    }

    @Override
    public boolean isActiveStudent(UUID accountId, UUID classId) {
        return false;
    }

    @Override
    public List<UUID> listActiveStudents(UUID classId) {
        return List.of();
    }

    @Override
    public boolean isClassManager(UUID accountId, UUID classId) {
        return false;
    }

    @Override
    public Map<SubjectStatus, Long> countSubjectsByStatus() {
        return Map.of();
    }

    @Override
    public Map<ClassStatus, Long> countClassesByStatus() {
        return Map.of();
    }

    @Override
    public long countActiveEnrollments() {
        return 0;
    }
}
