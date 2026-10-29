package edu.aiplatform.subjects.contract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * U04 khai báo, U05 cài: lớp của môn trên Subject Detail, số lớp trên Subject List, chặn lưu trữ môn
 * còn lớp chưa lưu trữ. Chưa có U05 thì danh sách rỗng.
 */
public interface ISubjectClasses {

    List<SubjectClassSummary> listClassesOfSubject(UUID subjectId);

    Map<UUID, Integer> countClasses(Collection<UUID> subjectIds);

    int countUnarchivedClasses(UUID subjectId);
}
