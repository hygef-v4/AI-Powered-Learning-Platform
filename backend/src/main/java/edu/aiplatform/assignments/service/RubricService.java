package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
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

    /**
     * Rubric "Toàn bài" (yêu cầu 2026-11-04): tiêu chí chấm cả bài, không gắn với mục nào; luôn có ở bài có mục, có thể trống.
     * Bài không có mục thì phần duy nhất đã là cả bài nên không thêm.
     */
    public static final String WHOLE = "ASSIGNMENT";
    public static final String WHOLE_TITLE = "Toàn bài";

    private final RubricRepository rubrics;
    private final AssignmentRepository assignments;

    public static boolean hasWhole(List<DocumentPart> parts) {
        return parts != null && !(parts.size() == 1 && parts.get(0).headingBlockId() == null);
    }

    public static DocumentPart createWholePart() {
        return new DocumentPart(WHOLE, null, List.of(), WHOLE_TITLE);
    }

    public RubricService(RubricRepository rubrics, AssignmentRepository assignments) {
        this.rubrics = rubrics;
        this.assignments = assignments;
    }

    /** Rubric theo thứ tự phần của bài; chỉ phần còn trong khung. */
    @Override
    @Transactional(readOnly = true)
    public List<RubricView> listRubrics(UUID assignmentId) {
        return assignments.findAssignment(assignmentId)
                .map(a -> orderRubrics(a, rubrics.listByAssignment(assignmentId)).stream().map(AssignmentRubric::view).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public RubricScore score(UUID rubricId, Set<UUID> checkedItemIds) {
        AssignmentRubric rubric = rubrics.findRubric(rubricId).orElseThrow(() -> ApiException.createNotFound("RUBRIC_NOT_FOUND"));
        Set<UUID> checked = checkedItemIds == null ? Set.of() : checkedItemIds;
        BigDecimal points = BigDecimal.ZERO;
        for (RubricItem item : rubric.listItems()) {
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

    List<AssignmentRubric> orderRubrics(Assignment assignment, List<AssignmentRubric> all) {
        Map<String, AssignmentRubric> byPart = all.stream()
                .collect(Collectors.toMap(AssignmentRubric::partId, r -> r, (a, b) -> a));
        List<AssignmentRubric> result = new ArrayList<>();
        // Toàn bài đứng đầu, chỉ khi đã có tiêu chí (chấm bài, điểm tối đa không có thẻ trống).
        AssignmentRubric whole = byPart.get(WHOLE);
        if (whole != null && hasWhole(assignment.parts()) && !listWrittenItems(whole).isEmpty()) {
            result.add(whole);
        }
        // Mục không có mục chấm nào không phải phần chấm (yêu cầu 2026-11-05): không có trong bảng chấm, điểm tối đa.
        for (DocumentPart part : assignment.parts()) {
            AssignmentRubric rubric = byPart.get(part.partId());
            if (rubric != null && !listWrittenItems(rubric).isEmpty()) {
                result.add(rubric);
            }
        }
        return result;
    }

    /** P1: phần mới có rubric trống, phần mất thì xóa rubric, phần còn giữ rubric đã điền (BR-U07-20). */
    void syncParts(UUID assignmentId, List<DocumentPart> parts, Instant now) {
        List<AssignmentRubric> existing = rubrics.listByAssignment(assignmentId);
        Set<String> wanted = parts.stream().map(DocumentPart::partId).collect(Collectors.toCollection(HashSet::new));
        if (hasWhole(parts)) {
            wanted.add(WHOLE);
        }
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
                rubrics.insert(AssignmentRubric.createEmptyRubric(assignmentId, part.partId(), now));
            }
        }
        if (hasWhole(parts) && !present.contains(WHOLE)) {
            rubrics.insert(AssignmentRubric.createEmptyRubric(assignmentId, WHOLE, now));
        }
    }

    /** Rubric Toàn bài của bài cũ chưa có thì tạo trống (khi người soạn sửa nó lần đầu). */
    AssignmentRubric ensureWhole(Assignment assignment, Instant now) {
        return findPartRubric(assignment, WHOLE).orElseGet(() -> {
            AssignmentRubric created = AssignmentRubric.createEmptyRubric(assignment.id(), WHOLE, now);
            rubrics.insert(created);
            return created;
        });
    }

    /** Bỏ mọi rubric rồi tạo theo phần mới, điền gợi ý nếu có (áp dụng đề xuất AI). */
    void replaceAll(Assignment assignment, Map<String, List<RubricCriterion>> suggestions, Instant now) {
        List<String> all = rubrics.listByAssignment(assignment.id()).stream().map(AssignmentRubric::partId).toList();
        rubrics.deleteParts(assignment.id(), all);
        for (DocumentPart part : assignment.parts()) {
            List<RubricCriterion> criteria = suggestions.getOrDefault(part.partId(), List.of());
            rubrics.insert(AssignmentRubric.createEmptyRubric(assignment.id(), part.partId(), now).replaceCriteria(criteria, now));
        }
        if (hasWhole(assignment.parts())) {
            rubrics.insert(AssignmentRubric.createEmptyRubric(assignment.id(), WHOLE, now)
                    .replaceCriteria(suggestions.getOrDefault(WHOLE, List.of()), now));
        }
    }

    void copy(UUID sourceAssignmentId, UUID targetAssignmentId, Instant now) {
        for (AssignmentRubric rubric : rubrics.listByAssignment(sourceAssignmentId)) {
            rubrics.insert(rubric.copyFor(targetAssignmentId, now));
        }
    }

    List<AssignmentRubric> listRubricModels(UUID assignmentId) {
        return rubrics.listByAssignment(assignmentId);
    }

    Optional<AssignmentRubric> findPartRubric(Assignment assignment, String partId) {
        return rubrics.listByAssignment(assignment.id()).stream().filter(r -> r.partId().equals(partId)).findFirst();
    }

    AssignmentRubric saveRubric(AssignmentRubric rubric) {
        return rubrics.saveRubric(rubric);
    }

    void lockAll(UUID assignmentId, Instant now) {
        for (AssignmentRubric r : rubrics.listByAssignment(assignmentId)) {
            if (!r.hasLocked()) {
                List<RubricCriterion> cleaned = stripBlankItems(r.criteria());
                if (!cleaned.equals(r.criteria())) {
                    rubrics.saveRubric(r.replaceCriteria(cleaned, now));
                }
            }
        }
        rubrics.lockAll(assignmentId, now);
    }

    static List<RubricCriterion> stripBlankItems(List<RubricCriterion> criteria) {
        return criteria.stream()
                .map(c -> new RubricCriterion(c.criterionId(), c.title(), c.description(), c.kind(),
                        c.items() == null ? List.of() : c.items().stream().filter(RubricService::isWritten).toList()))
                .filter(c -> !c.items().isEmpty())
                .toList();
    }

    /**
     * Chuẩn hóa tiêu chí khi lưu (BR-U07-21): giới hạn số lượng, độ dài; điểm nếu có phải ≥ 0 (0 điểm được, 2026-11-04), tối đa 2 chữ số thập
     * phân; {@code DIAGRAM} chỉ khi bật sơ đồ. Bản nháp được thiếu tiêu chí, mục, điểm. Cấp id cho mục mới.
     */
    static List<RubricCriterion> normalize(List<RubricCriterion> criteria, boolean diagramEnabled) {
        if (criteria == null) {
            throw ApiException.createBadRequest("RUBRIC_INVALID", "Rubric không hợp lệ");
        }
        if (criteria.size() > AssignmentRubric.CRITERIA_MAX) {
            throw ApiException.createBadRequest("RUBRIC_TOO_MANY_CRITERIA", "Rubric tối đa 20 tiêu chí");
        }
        Set<UUID> ids = new HashSet<>();
        List<RubricCriterion> out = new ArrayList<>();
        for (RubricCriterion criterion : criteria) {
            if (criterion == null) {
                throw ApiException.createBadRequest("RUBRIC_INVALID", "Tiêu chí không hợp lệ");
            }
            CriterionKind kind = criterion.kind() == null ? CriterionKind.TEXT : criterion.kind();
            if (kind == CriterionKind.DIAGRAM && !diagramEnabled) {
                throw ApiException.createBadRequest("RUBRIC_DIAGRAM_DISABLED", "Bài chưa bật sơ đồ, không dùng tiêu chí sơ đồ");
            }
            String title = limit(criterion.title(), AssignmentRubric.TITLE_MAX, "Tên tiêu chí tối đa 200 ký tự");
            String description = limit(criterion.description(), AssignmentRubric.DESCRIPTION_MAX,
                    "Mô tả tiêu chí tối đa 2.000 ký tự");
            out.add(new RubricCriterion(ensureUniqueId(criterion.criterionId(), ids), title, description, kind,
                    normalizeItems(criterion.items(), ids)));
        }
        return List.copyOf(out);
    }

    private static List<RubricItem> normalizeItems(List<RubricItem> raw, Set<UUID> ids) {
        List<RubricItem> items = raw == null ? List.of() : raw;
        if (items.size() > AssignmentRubric.ITEMS_MAX) {
            throw ApiException.createBadRequest("RUBRIC_TOO_MANY_ITEMS", "Mỗi tiêu chí tối đa 20 mục chấm");
        }
        List<RubricItem> clean = new ArrayList<>();
        for (RubricItem item : items) {
            if (item == null) {
                throw ApiException.createBadRequest("RUBRIC_INVALID", "Mục chấm không hợp lệ");
            }
            BigDecimal points = item.points();
            if (points != null && (points.signum() < 0 || points.stripTrailingZeros().scale() > 2
                    || points.compareTo(AssignmentRubric.POINTS_MAX) > 0)) {
                throw ApiException.createBadRequest("RUBRIC_POINTS_INVALID",
                        "Điểm mỗi mục phải lớn hơn 0, tối đa 2 chữ số thập phân");
            }
            clean.add(new RubricItem(ensureUniqueId(item.itemId(), ids),
                    limit(item.text(), AssignmentRubric.ITEM_TEXT_MAX, "Nội dung mục chấm tối đa 1.000 ký tự"),
                    points == null ? null : points.setScale(2, java.math.RoundingMode.UNNECESSARY)));
        }
        return List.copyOf(clean);
    }

    static final String MISSING = "RUBRIC_MISSING";
    static final String POINTS_MISSING = "RUBRIC_POINTS_MISSING";
    static final String WHOLE_POINTS_MISSING = "RUBRIC_WHOLE_POINTS_MISSING";

    /**
     * Lỗi chặn phát hành về rubric (yêu cầu 2026-11-05):
     * <ul>
     * <li>Cả bài tổng điểm bằng 0: chỉ báo "Bài làm chưa có rubric chấm điểm", không tô đỏ thẻ nào (kể cả Toàn bài).
     * Bài Luyện tập không bắt buộc rubric nên không báo lỗi này.</li>
     * <li>Thẻ có mục chấm mà tổng điểm không quá 0, hoặc có mục ghi chữ chưa ghi điểm hay ghi điểm chưa ghi chữ: "Rubric
     * thiếu điểm" trên thẻ đó.</li>
     * <li>Dòng không chữ không điểm bỏ qua; thẻ chỉ có dòng như vậy coi như chưa có rubric.</li>
     * </ul>
     * Thẻ Toàn bài (và phần duy nhất của bài không có mục) báo không gắn tiêu đề, mã {@code RUBRIC_WHOLE_POINTS_MISSING}.
     */
    List<ValidationIssue> listReadinessIssues(Assignment assignment) {
        Map<String, AssignmentRubric> byPart = new HashMap<>();
        rubrics.listByAssignment(assignment.id()).forEach(r -> byPart.put(r.partId(), r));
        List<ValidationIssue> issues = new ArrayList<>();
        List<Card> cards = new ArrayList<>();
        if (hasWhole(assignment.parts()) && byPart.containsKey(WHOLE)) {
            cards.add(new Card(byPart.get(WHOLE), null, "Toàn bài"));
        }
        for (DocumentPart part : assignment.parts()) {
            AssignmentRubric rubric = byPart.get(part.partId());
            if (rubric != null) {
                cards.add(new Card(rubric, part.headingBlockId(), part.title()));
            }
        }
        BigDecimal total = cards.stream().map(card -> sumPoints(card.rubric())).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Bài Luyện tập không được chấm (BR-U09-03) nên không bắt buộc rubric, nhưng thẻ đã ghi vẫn kiểm như dưới
        // (BR-U07-32, KAN-225); thẻ trống không báo lỗi.
        if (total.signum() <= 0 && assignment.gradingMode() != GradingMode.PRACTICE) {
            issues.add(new ValidationIssue(MISSING, null, "Bài làm chưa có rubric chấm điểm"));
            return issues;
        }
        for (Card card : cards) {
            if (isPointsMissing(card.rubric())) {
                String code = card.blockId() == null ? WHOLE_POINTS_MISSING : POINTS_MISSING;
                // Ghi rõ lý do trên thẻ (yêu cầu 2026-11-05): mục ghi dở, tổng điểm không quá 0.
                if (isHalfWritten(card.rubric())) {
                    issues.add(new ValidationIssue(code, card.blockId(), "Có mục chấm thiếu nội dung hoặc thiếu điểm"));
                }
                if (sumPoints(card.rubric()).signum() <= 0) {
                    issues.add(new ValidationIssue(code, card.blockId(), "Tổng điểm của rubric phải lớn hơn 0"));
                }
            } else if (!assignment.diagramEnabled() && card.rubric().hasDiagramCriteria()) {
                issues.add(new ValidationIssue("RUBRIC_DIAGRAM_DISABLED", card.blockId(),
                        "Mục \"" + card.title() + "\" có tiêu chí sơ đồ nhưng bài tắt sơ đồ"));
            }
        }
        return issues;
    }

    private record Card(AssignmentRubric rubric, UUID blockId, String title) {
    }

    /** Mục chấm có ghi gì đó (chữ hoặc điểm); dòng trống hẳn bỏ qua. */
    private static List<RubricItem> listWrittenItems(AssignmentRubric rubric) {
        return rubric.criteria().stream().flatMap(c -> c.items() == null ? java.util.stream.Stream.<RubricItem>empty()
                : c.items().stream()).filter(RubricService::isWritten).toList();
    }

    private static boolean isWritten(RubricItem item) {
        return !(item.text() == null || item.text().isBlank()) || item.points() != null;
    }

    private static BigDecimal sumPoints(AssignmentRubric rubric) {
        return listWrittenItems(rubric).stream().map(RubricItem::points).filter(p -> p != null && p.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Thẻ có mục chấm mà tổng điểm không quá 0, hoặc mục ghi dở (chữ thiếu điểm, điểm thiếu chữ). */
    static boolean isPointsMissing(AssignmentRubric rubric) {
        List<RubricItem> items = listWrittenItems(rubric);
        if (items.isEmpty()) {
            return false;
        }
        return isHalfWritten(rubric) || sumPoints(rubric).signum() <= 0;
    }

    private static boolean isHalfWritten(AssignmentRubric rubric) {
        return listWrittenItems(rubric).stream().anyMatch(item -> item.text() == null || item.text().isBlank()
                || item.points() == null || item.points().signum() < 0);
    }

    private static UUID ensureUniqueId(UUID requested, Set<UUID> ids) {
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
            throw ApiException.createBadRequest("RUBRIC_TEXT_TOO_LONG", message);
        }
        return text;
    }
}
