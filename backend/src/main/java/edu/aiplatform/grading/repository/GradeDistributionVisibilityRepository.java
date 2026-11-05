package edu.aiplatform.grading.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GradeDistributionVisibilityRepository {
    public record Visibility(boolean enabled, long version) { }

    private final JdbcTemplate jdbcTemplate;

    public GradeDistributionVisibilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Visibility findVisibility(UUID classId, UUID assignmentId) {
        return jdbcTemplate.query("SELECT enabled, version FROM grade_distribution_visibility WHERE class_id=? AND assignment_id=?",
                (rs, row) -> new Visibility(rs.getBoolean("enabled"), rs.getLong("version")), classId, assignmentId)
                .stream().findFirst().orElse(new Visibility(false, 0));
    }

    public boolean updateVisibility(UUID classId, UUID assignmentId, boolean enabled, long version) {
        if (version == 0) {
            return jdbcTemplate.update("INSERT INTO grade_distribution_visibility (class_id, assignment_id, enabled, version) VALUES (?,?,?,1) ON CONFLICT DO NOTHING",
                    classId, assignmentId, enabled) == 1;
        }
        return jdbcTemplate.update("UPDATE grade_distribution_visibility SET enabled=?, version=version+1 WHERE class_id=? AND assignment_id=? AND version=?",
                enabled, classId, assignmentId, version) == 1;
    }
}
