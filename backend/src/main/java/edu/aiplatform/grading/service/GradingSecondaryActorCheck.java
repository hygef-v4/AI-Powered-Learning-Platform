package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.RubricView;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class GradingSecondaryActorCheck {
    private static final String SECONDARY_ACTOR = "(?:secondary actors?|actor phụ|tác nhân phụ)";
    private static final Pattern ACTOR_FIELD = Pattern.compile(SECONDARY_ACTOR + "\\s*:?");
    // shortcut: only explicit "(names) ghi đúng" criteria, expand when rubrics expose structured actor requirements.
    private static final Pattern REQUIRED_ACTORS = Pattern.compile(
            "^(?:tên use case [^,;():]+, (?:primary actor|actor chính|tác nhân chính) [^,;():]+ và )?"
                    + SECONDARY_ACTOR + " \\(([\\p{L}\\p{M}\\p{N} ,;.'’-]+)\\) ghi đúng\\.?$");
    // (?U): Unicode \b, otherwise a word ending in a Vietnamese letter such as "dụ" never matches.
    private static final Pattern NON_MANDATORY = Pattern.compile(
            "(?U)\\b(?:hoặc|hay|or|nếu|tùy chọn|optional|có thể|tương đương|ví dụ|chẳng hạn|such as|e\\.g|v\\.v)\\b");
    private static final Pattern SEPARATOR = Pattern.compile("[,;\\r\\n]+|\\s+(?:và|and)\\s+");
    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

    // Ngăn tạo đối tượng cho lớp chỉ cung cấp các hàm tĩnh.
    private GradingSecondaryActorCheck() {
    }

    // Tìm các tiêu chí đã đánh dấu nhưng bằng chứng trong ô tác nhân không khớp.
    static Set<String> findMismatchedItems(RubricView rubric, List<DocumentBlock> blocks,
            JsonNode checkedItems) {
        if (!checkedItems.isArray() || checkedItems.isEmpty()) {
            return Set.of();
        }
        String actorCell = findActorCell(blocks);
        if (actorCell == null) {
            return Set.of();
        }
        Set<String> observed = splitActorNames(actorCell);
        Set<String> fullyAwarded = new HashSet<>();
        checkedItems.forEach(item -> fullyAwarded.add(item.asText().toLowerCase(Locale.ROOT)));
        Set<String> mismatches = new HashSet<>();
        for (var criterion : rubric.criteria()) {
            if (criterion.kind() != CriterionKind.TEXT) {
                continue;
            }
            for (var item : criterion.items()) {
                String itemId = item.itemId().toString();
                if (!fullyAwarded.contains(itemId) || item.points().signum() <= 0) {
                    continue;
                }
                String text = normalizeText(item.text());
                var required = REQUIRED_ACTORS.matcher(text);
                if (required.matches() && !NON_MANDATORY.matcher(text).find()
                        && !observed.containsAll(splitActorNames(required.group(1)))) {
                    mismatches.add(itemId);
                }
            }
        }
        return mismatches;
    }

    // Tìm giá trị ô mô tả tác nhân trong dữ liệu bảng của bài nộp.
    private static String findActorCell(List<DocumentBlock> blocks) {
        String observed = null;
        for (var block : blocks) {
            if (block.type() != BlockType.TABLE || block.content() == null) {
                continue;
            }
            for (var row : block.content().path("rows")) {
                if (row.isArray() && row.size() == 2 && ACTOR_FIELD.matcher(normalizeText(
                        GradingPromptParts.joinRunText(row.get(0).path("runs")))).matches()) {
                    if (observed != null) {
                        return null;
                    }
                    observed = GradingPromptParts.joinRunText(row.get(1).path("runs"));
                }
            }
        }
        return observed;
    }

    // Tách các tên đã khai báo, chuẩn hóa và bỏ mục trống trước khi đối chiếu.
    private static Set<String> splitActorNames(String text) {
        return Arrays.stream(SEPARATOR.split(text.toLowerCase(Locale.ROOT)))
                .map(GradingSecondaryActorCheck::normalizeText)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());
    }

    // Chuẩn hóa Unicode, chữ hoa/thường và khoảng trắng để so sánh nội dung.
    private static String normalizeText(String text) {
        return WHITESPACE.matcher(Normalizer.normalize(text, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }
}
