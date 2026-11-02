package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.IRubric;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricScore;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.assignments.repository.RubricRepository;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IRubric} và quy tắc rubric (BR-U07-20…25): mỗi phần một rubric tự tạo, tự xóa; kiểm giới hạn và điểm;
 * tổng tự tính; khóa khi phát hành; nhân bản thành rubric mới; tính điểm theo mục đạt.
 */
@Service
public class RubricService implements IRubric {

    private final RubricRepository rubrics;
    private final AssignmentRepository assignments;

    public RubricService(RubricRepository rubrics, AssignmentRepository assignments) {
        this.rubrics = rubrics;
        this.assignments = assignments;
    }

    /** Rubric theo thứ tự phần của bài; chỉ phần còn trong khung. */
    @Override
    @Transactional(readOnly = true)
    public List<RubricView> listRubrics(UUID assignmentId) {
        return assignments.find(assignmentId)
                .map(a -> ordered(a, rubrics.listByAssignment(assignmentId)).stream().map(AssignmentRubric::view).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public RubricScore score(UUID rubricId, Set<UUID> checkedItemIds) {
        AssignmentRubric rubric = rubrics.find(rubricId).orElseThrow(() -> ApiException.notFound("RUBRIC_NOT_FOUND"));
        Set<UUID> checked = checkedItemIds == null ? Set.of() : checkedItemIds;
        BigDecimal points = BigDecimal.ZERO;
        for (RubricItem item : rubric.items()) {
            if (item.points() != null && checked.contains(item.itemId())) {
                points = points.add(item.points());
            }
        }
        return new RubricScore(rubricId, points.setScale(2, java.math.RoundingMode.HALF_UP), rubric.totalPoints());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal maxPoints(UUID assignmentId) {
        return listRubrics(assignmentId).stream().map(RubricView::maxPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    List<AssignmentRubric> ordered(Assignment assignment, List<AssignmentRubric> all) {
        Map<String, AssignmentRubric> byPart = all.stream()
                .collect(Collectors.toMap(AssignmentRubric::partId, r -> r, (a, b) -> a));
        List<AssignmentRubric> result = new ArrayList<>();
        for (DocumentPart part : assignment.parts()) {
            AssignmentRubric rubric = byPart.get(part.partId());
            if (rubric != null) {
                result.add(rubric);
            }
        }
        return result;
    }

    /** P1: phần mới có rubric trống, phần mất thì xóa rubric, phần còn giữ rubric đã điền (BR-U07-20). */
    void syncParts(UUID assignmentId, List<DocumentPart> parts, Instant now) {
        List<AssignmentRubric> existing = rubrics.listByAssignment(assignmentId);
        Set<String> wanted = parts.stream().map(DocumentPart::partId).collect(Collectors.toSet());
        Set<String> present = new HashSet<>();
        List<String> gone = new ArrayList<>();
        for (AssignmentRubric rubric : existing) {
            if (wanted.contains(rubric.partId())) {
                present.add(rubric.partId());
            } else {
                gone.add(rubric.partId());
            }
        }
        rubrics.deleteParts(assignmentId, gone);
        for (DocumentPart part : parts) {
            if (!present.contains(part.partId())) {
                rubrics.insert(AssignmentRubric.empty(assignmentId, part.partId(), now));
            }
        }
    }

    /** Bỏ mọi rubric rồi tạo theo phần mới, điền gợi ý nếu có (áp dụng đề xuất AI). */
    void replaceAll(Assignment assignment, Map<String, List<RubricCriterion>> suggestions, Instant now) {
        List<String> all = rubrics.listByAssignment(assignment.id()).stream().map(AssignmentRubric::partId).toList();
        rubrics.deleteParts(assignment.id(), all);
        for (DocumentPart part : assignment.parts()) {
            List<RubricCriterion> criteria = suggestions.getOrDefault(part.partId(), List.of());
            rubrics.insert(AssignmentRubric.empty(assignment.id(), part.partId(), now).withCriteria(criteria, now));
        }
    }

    void copy(UUID sourceAssignmentId, UUID targetAssignmentId, Instant now) {
        for (AssignmentRubric rubric : rubrics.listByAssignment(sourceAssignmentId)) {
            rubrics.insert(rubric.copyFor(targetAssignmentId, now));
        }
    }

    List<AssignmentRubric> all(UUID assignmentId) {
        return rubrics.listByAssignment(assignmentId);
    }

    Optional<AssignmentRubric> forPart(Assignment assignment, String partId) {
        return rubrics.listByAssignment(assignment.id()).stream().filter(r -> r.partId().equals(partId)).findFirst();
    }

    AssignmentRubric save(AssignmentRubric rubric) {
        return rubrics.save(rubric);
    }

    void lockAll(UUID assignmentId, Instant now) {
        rubrics.lockAll(assignmentId, now);
    }

    /**
     * Chuẩn hóa tiêu chí khi lưu (BR-U07-21): giới hạn số lượng, độ dài; điểm nếu có phải > 0, tối đa 2 chữ số thập
     * phân; {@code DIAGRAM} chỉ khi bật sơ đồ. Bản nháp được thiếu tiêu chí, mục, điểm. Cấp id cho mục mới.
     */
    static List<RubricCriterion> normalize(List<RubricCriterion> criteria, boolean diagramEnabled) {
        if (criteria == null) {
            throw ApiException.badRequest("RUBRIC_INVALID", "Rubric không hợp lệ");
        }
        if (criteria.size() > AssignmentRubric.CRITERIA_MAX) {
            throw ApiException.badRequest("RUBRIC_TOO_MANY_CRITERIA", "Rubric tối đa 20 tiêu chí");
        }
        Set<UUID> ids = new HashSet<>();
        List<RubricCriterion> out = new ArrayList<>();
        for (RubricCriterion criterion : criteria) {
            if (criterion == null) {
                throw ApiException.badRequest("RUBRIC_INVALID", "Tiêu chí không hợp lệ");
            }
            CriterionKind kind = criterion.kind() == null ? CriterionKind.TEXT : criterion.kind();
            if (kind == CriterionKind.DIAGRAM && !diagramEnabled) {
                throw ApiException.badRequest("RUBRIC_DIAGRAM_DISABLED", "Bài chưa bật sơ đồ, không dùng tiêu chí sơ đồ");
            }
            String title = limit(criterion.title(), AssignmentRubric.TITLE_MAX, "Tên tiêu chí tối đa 200 ký tự");
            String description = limit(criterion.description(), AssignmentRubric.DESCRIPTION_MAX,
                    "Mô tả tiêu chí tối đa 2.000 ký tự");
            List<RubricItem> items = criterion.items() == null ? List.of() : criterion.items();
            if (items.size() > AssignmentRubric.ITEMS_MAX) {
                throw ApiException.badRequest("RUBRIC_TOO_MANY_ITEMS", "Mỗi tiêu chí tối đa 20 mục chấm");
            }
            List<RubricItem> cleanItems = new ArrayList<>();
            for (RubricItem item : items) {
                if (item == null) {
                    throw ApiException.badRequest("RUBRIC_INVALID", "Mục chấm không hợp lệ");
                }
                BigDecimal points = item.points();
                if (points != null && (points.signum() <= 0 || points.stripTrailingZeros().scale() > 2
                        || points.compareTo(AssignmentRubric.POINTS_MAX) > 0)) {
                    throw ApiException.badRequest("RUBRIC_POINTS_INVALID",
                            "Điểm mỗi mục phải lớn hơn 0, tối đa 2 chữ số thập phân");
                }
                UUID itemId = unique(item.itemId(), ids);
                cleanItems.add(new RubricItem(itemId,
                        limit(item.text(), AssignmentRubric.ITEM_TEXT_MAX, "Nội dung mục chấm tối đa 1.000 ký tự"),
                        points == null ? null : points.setScale(2, java.math.RoundingMode.UNNECESSARY)));
            }
            out.add(new RubricCriterion(unique(criterion.criterionId(), ids), title, description, kind,
                    List.copyOf(cleanItems)));
        }
        return List.copyOf(out);
    }

    /** Lỗi chặn duyệt, phát hành: phần thiếu rubric hoặc rubric chưa đủ; tiêu chí sơ đồ khi tắt sơ đồ. */
    List<ValidationIssue> readinessIssues(Assignment assignment) {
        Map<String, AssignmentRubric> byPart = new HashMap<>();
        rubrics.listByAssignment(assignment.id()).forEach(r -> byPart.put(r.partId(), r));
        List<ValidationIssue> issues = new ArrayList<>();
        for (DocumentPart part : assignment.parts()) {
            AssignmentRubric rubric = byPart.get(part.partId());
            if (rubric == null || !rubric.complete()) {
                issues.add(new ValidationIssue("RUBRIC_INCOMPLETE", part.headingBlockId(),
                        "Mục \"" + part.title() + "\" chưa có rubric đủ tiêu chí và điểm"));
            } else if (!assignment.diagramEnabled() && rubric.hasDiagramCriteria()) {
                issues.add(new ValidationIssue("RUBRIC_DIAGRAM_DISABLED", part.headingBlockId(),
                        "Mục \"" + part.title() + "\" có tiêu chí sơ đồ nhưng bài tắt sơ đồ"));
            }
        }
        return issues;
    }

    private static UUID unique(UUID requested, Set<UUID> ids) {
        UUID id = requested == null || ids.contains(requested) ? UUID.randomUUID() : requested;
        ids.add(id);
        return id;
    }

    private static String limit(String value, int max, String message) {
        if (value == null) {
            return "";
        }
        String text = value.strip();
        if (text.length() > max) {
            throw ApiException.badRequest("RUBRIC_TEXT_TOO_LONG", message);
        }
        return text;
    }
}
