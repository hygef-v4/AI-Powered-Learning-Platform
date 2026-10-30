package edu.aiplatform.subjects.repository;

import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.model.Subject;
import edu.aiplatform.subjects.model.SubjectCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Mapping riêng U04; không trả managed entity ra khỏi adapter hoặc sang unit khác. */
@Entity
@Table(name = "subjects")
public class SubjectJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20, updatable = false)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubjectStatus status;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected SubjectJpaEntity() {
    }

    static SubjectJpaEntity forInsert(Subject subject) {
        if (subject.version() != 1) {
            throw new IllegalArgumentException("Môn mới phải có public version 1");
        }
        SubjectJpaEntity row = new SubjectJpaEntity();
        row.id = subject.id();
        row.code = subject.code().value();
        row.name = subject.name();
        row.description = subject.description();
        row.status = subject.status();
        row.managerId = subject.managerId();
        row.createdAt = subject.createdAt();
        row.updatedAt = subject.updatedAt();
        // Hibernate khởi tạo @Version=null thành 0 lúc persist; không gán public version vào đây.
        return row;
    }

    Subject toDomain() {
        return new Subject(id, new SubjectCode(code), name, description, status, managerId,
                createdAt, updatedAt, Math.addExact(version, 1));
    }
}
