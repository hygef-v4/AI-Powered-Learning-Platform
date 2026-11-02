package edu.aiplatform.assignments.domain;

import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Rubric của một phần (BR-U07-20…25). Điểm tối đa là tổng điểm các mục, tự tính; mục chưa có điểm (bản nháp)
 * không cộng. {@code lockedAt} đặt khi phát hành, sau đó không sửa, không xóa.
 */
public record AssignmentRubric(UUID id, UUID assignmentId, String partId, List<RubricCriterion> criteria,
                               BigDecimal totalPoints, Instant lockedAt, Instant createdAt, Instant updatedAt,
                               long version) {

    public static final int CRITERIA_MAX = 20;
    public static final int ITEMS_MAX = 20;
    public static final int TITLE_MAX = 200;
    public static final int DESCRIPTION_MAX = 2_000;
    public static final int ITEM_TEXT_MAX = 1_000;
    public static final BigDecimal POINTS_MAX = new BigDecimal("1000");

    public AssignmentRubric {
        criteria = List.copyOf(criteria);
    }

    public static AssignmentRubric empty(UUID assignmentId, String partId, Instant now) {
        return new AssignmentRubric(UUID.randomUUID(), assignmentId, partId, List.of(), BigDecimal.ZERO, null, now,
                now, 0);
    }

    public boolean locked() {
        return lockedAt != null;
    }

    public AssignmentRubric withCriteria(List<RubricCriterion> next, Instant now) {
        return new AssignmentRubric(id, assignmentId, partId, next, total(next), lockedAt, createdAt, now, version);
    }

    /** Bản mới, chưa khóa, của bài đích (BR-U07-25); id tiêu chí, mục giữ nguyên trong rubric mới. */
    public AssignmentRubric copyFor(UUID targetAssignmentId, Instant now) {
        return new AssignmentRubric(UUID.randomUUID(), targetAssignmentId, partId, criteria, totalPoints, null, now,
                now, 0);
    }

    /** Đủ để duyệt, phát hành: 1–20 tiêu chí, mỗi tiêu chí 1–20 mục có nội dung và điểm (BR-U07-21, 24). */
    public boolean complete() {
        if (criteria.isEmpty() || criteria.size() > CRITERIA_MAX) {
            return false;
        }
        for (RubricCriterion criterion : criteria) {
            if (blank(criterion.title()) || criterion.items() == null || criterion.items().isEmpty()
                    || criterion.items().size() > ITEMS_MAX) {
                return false;
            }
            for (RubricItem item : criterion.items()) {
                if (blank(item.text()) || item.points() == null || item.points().signum() <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasDiagramCriteria() {
        return criteria.stream().anyMatch(c -> c.kind() == CriterionKind.DIAGRAM);
    }

    public RubricView view() {
        return new RubricView(id, assignmentId, partId, criteria, totalPoints);
    }

    public static BigDecimal total(List<RubricCriterion> criteria) {
        BigDecimal sum = BigDecimal.ZERO;
        for (RubricCriterion criterion : criteria) {
            for (RubricItem item : criterion.items() == null ? List.<RubricItem>of() : criterion.items()) {
                if (item.points() != null) {
                    sum = sum.add(item.points());
                }
            }
        }
        return sum.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Mọi id mục chấm, theo thứ tự; dùng khi tính điểm. */
    public List<RubricItem> items() {
        List<RubricItem> all = new ArrayList<>();
        criteria.forEach(c -> all.addAll(Objects.requireNonNullElse(c.items(), List.of())));
        return all;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
