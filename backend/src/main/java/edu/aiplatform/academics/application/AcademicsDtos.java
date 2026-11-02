package edu.aiplatform.academics.application;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.domain.*;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.util.*;

public final class AcademicsDtos {
    private AcademicsDtos() { }
    public record Person(UUID id, String name, String email) { }
    public record Subject(UUID id, String code, String name) { }
    public record SubjectSummary(UUID id, String code, String name, SubjectStatus status, Map<ClassStatus,Long> classCounts) { }
    public record Student(UUID accountId, String name, String email, Instant enrolledAt, EnrollmentStatus status) { }
    public record ClassView(UUID id, Subject subject, String code, String name, String description, String term,
                            ClassStatus status, Person teacher, long activeStudentCount, long removedStudentCount,
                            Instant createdAt, boolean showGradeDistribution, long version) { }
    public record RowResult(int line, String email, EnrollmentResult result) { }
}
