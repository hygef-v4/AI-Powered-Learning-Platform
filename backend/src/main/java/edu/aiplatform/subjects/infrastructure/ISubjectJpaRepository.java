package edu.aiplatform.subjects.infrastructure;

import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Chỉ adapter U04 sử dụng; unit khác dùng ISubjectQuery. */
public interface ISubjectJpaRepository extends JpaRepository<SubjectJpaEntity, UUID> {

    boolean existsByCode(String code);

    boolean existsByIdAndManagerId(UUID id, UUID managerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubjectJpaEntity s where s.id = :id")
    Optional<SubjectJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select s from SubjectJpaEntity s
            where (:status is null or s.status = :status)
              and (lower(s.code) like :pattern escape '!' or lower(s.name) like :pattern escape '!')
            """)
    List<SubjectJpaEntity> findPage(@Param("pattern") String pattern, @Param("status") SubjectStatus status,
                                    Pageable page);

    @Query("""
            select count(s) from SubjectJpaEntity s
            where (:status is null or s.status = :status)
              and (lower(s.code) like :pattern escape '!' or lower(s.name) like :pattern escape '!')
            """)
    long countMatching(@Param("pattern") String pattern, @Param("status") SubjectStatus status);

    List<SubjectJpaEntity> findByManagerIdOrderByCodeAsc(UUID managerId);

    /**
     * Atomic compare-and-set: không merge snapshot cũ, không cập nhật code/createdAt.
     * Không clear toàn persistence context vì transaction caller có thể đang sửa account U01.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update SubjectJpaEntity s
               set s.name = :name, s.description = :description, s.status = :status,
                   s.managerId = :managerId, s.updatedAt = :updatedAt, s.version = :nextVersion
             where s.id = :id and s.version = :expectedVersion and s.code = :code
            """)
    int updateSnapshot(@Param("id") UUID id, @Param("code") String code, @Param("name") String name,
                       @Param("description") String description, @Param("status") SubjectStatus status,
                       @Param("managerId") UUID managerId, @Param("updatedAt") Instant updatedAt,
                       @Param("expectedVersion") long expectedVersion, @Param("nextVersion") long nextVersion);
}
