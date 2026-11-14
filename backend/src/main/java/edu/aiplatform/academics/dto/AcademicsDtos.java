package edu.aiplatform.academics.dto;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.model.*;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.util.*;

public final class AcademicsDtos {
    /** Prevents instantiation of this DTO container. */
    private AcademicsDtos() {
    }

    /** {@code studentCode}: MSSV của sinh viên, rỗng với giảng viên hoặc khi chưa có (BR-U05-60). */
    public record Person(UUID id, String name, String email, String studentCode) {

        public Person(UUID id, String name, String email) {
            this(id, name, email, null);
        }
    }

    public record Subject(UUID id, String code, String name) {
    }

    public record SubjectSummary(UUID id, String code, String name, SubjectStatus status,
            Map<ClassStatus, Long> classCounts) {
    }

    public record Student(UUID accountId, String name, String email, Instant enrolledAt,
            EnrollmentStatus status, String studentCode) {

        public Student(UUID accountId, String name, String email, Instant enrolledAt,
                EnrollmentStatus status) {
            this(accountId, name, email, enrolledAt, status, null);
        }
    }

    public record ClassView(UUID id, Subject subject, String code, String name, String description,
            ClassStatus status, Person teacher, long activeStudentCount,
            long removedStudentCount, Instant createdAt, boolean showGradeDistribution,
            long version) {
    }

    /** {@code email} giữ tên trường cũ của API, chứa đúng giá trị đã nhập: email hoặc MSSV (BR-U05-58). */
    public record RowResult(int line, String email, EnrollmentResult result) {
    }
}
