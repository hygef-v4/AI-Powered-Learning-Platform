package edu.aiplatform.subjects.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * U04 khai báo, U05 cài: lớp của môn trên Subject Detail, số lớp trên Subject List, lưu trữ mọi lớp khi lưu trữ môn.
 * Chưa có U05 hoặc query lỗi thì báo không khả dụng (Q05), không giả danh sách rỗng/0.
 */
public interface ISubjectClasses {

    List<SubjectClassSummary> listClassesOfSubject(UUID subjectId);

    Map<UUID, Integer> countClasses(Collection<UUID> subjectIds);

    int countUnarchivedClasses(UUID subjectId);

    /**
     * Lưu trữ mọi lớp {@code DRAFT}, {@code OPEN} của môn (BR-U04-04), giữ nguyên ghi danh (BR-U05-16), ghi audit
     * {@code CLASS_STATUS_CHANGED} từng lớp với người thực hiện {@code actor}. U04 gọi trong transaction lưu trữ môn,
     * khi đã khóa dòng môn; lỗi thì ném để cả việc lưu trữ môn rollback. Trả id các lớp đã lưu trữ.
     */
    List<UUID> archiveClassesOfSubject(UUID subjectId, Actor actor);
}
