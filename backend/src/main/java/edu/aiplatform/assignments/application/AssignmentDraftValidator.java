package edu.aiplatform.assignments.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.domain.AssignmentRubric;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Kiểm những điều có thể xác định chắc chắn trước khi đề xuất được áp dụng vào bài. */
final class AssignmentDraftValidator {

    private AssignmentDraftValidator() {
    }

    static ObjectNode validate(JsonNode proposal, GradingMode gradingMode, Options options) {
        ArrayNode issues = JsonNodeFactory.instance.arrayNode();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        int questionCount = 0;
        int leafCount = 0;
        JsonNode sections = proposal == null ? null : proposal.path("sections");

        if (proposal != null && options.requireTitle()) {
            if (proposal.path("businessContext").asText("").length() > AiDraftProposal.MAX_GENERATED_CONTEXT) {
                issue(issues, "ERROR", "CONTEXT_TOO_LONG", "businessContext",
                        "Bối cảnh AI quá dài; tạo lại với mô tả ngắn, tách đoạn và không lặp nội dung.");
            }
            if (proposal.path("instructions").asText("").length() > AiDraftProposal.MAX_GENERATED_INSTRUCTIONS) {
                issue(issues, "ERROR", "INSTRUCTIONS_TOO_LONG", "instructions",
                        "Hướng dẫn AI quá dài; chỉ giữ quy định làm và nộp bài.");
            }
        }

        if (proposal == null || !proposal.isObject() || !sections.isArray() || sections.isEmpty()) {
            issue(issues, "ERROR", "PROPOSAL_STRUCTURE_INVALID", "sections",
                    "Đề xuất cần có ít nhất một câu hỏi hợp lệ.");
        } else {
            String profile = options.profile() == null ? "GENERAL" : options.profile();
            if ((profile.equals("AUTO") || profile.equals("SWR") || profile.equals("SWD"))
                    && proposal.path("businessContext").asText("").isBlank()) {
                issue(issues, "ERROR", "BUSINESS_CONTEXT_REQUIRED", "businessContext",
                        "Đề SWR/SWD cần có bối cảnh nghiệp vụ để sinh viên trả lời được câu hỏi.");
            }
            if (options.requireTitle() && proposal.path("title").asText("").isBlank()) {
                issue(issues, "ERROR", "TITLE_REQUIRED", "title", "Đề xuất cần có tiêu đề.");
            }
            if (sections.size() > AiDraftProposal.MAX_SECTIONS) {
                issue(issues, "ERROR", "SECTION_LIMIT_EXCEEDED", "sections",
                        "Đề có quá nhiều mục.");
            }

            List<Integer> levels = new ArrayList<>(sections.size());
            for (int index = 0; index < sections.size(); index++) {
                JsonNode section = sections.get(index);
                int level = section.path("level").asInt(1);
                if (!section.isObject() || !section.path("level").canConvertToInt() || level < 1 || level > 6
                        || section.path("title").asText("").isBlank()) {
                    issue(issues, "ERROR", "SECTION_INVALID", "sections[" + index + "]",
                            "Mỗi câu cần tiêu đề và cấp mục từ 1 đến 6.");
                }
                levels.add(Math.max(1, Math.min(6, level)));
                if (level == 1) {
                    questionCount++;
                }
            }

            for (int index = 0; index < sections.size(); index++) {
                JsonNode section = sections.get(index);
                boolean leaf = index + 1 == sections.size() || levels.get(index + 1) <= levels.get(index);
                JsonNode rubric = section.path("rubric");
                if (!leaf) {
                    if (rubric.isArray() && !rubric.isEmpty()) {
                        issue(issues, "ERROR", "RUBRIC_ON_NON_LEAF", "sections[" + index + "].rubric",
                                "Tiêu chí đặt ở mục cha sẽ không gắn với phần chấm; chuyển chúng xuống câu con.");
                    }
                    continue;
                }
                leafCount++;
                if (!rubric.isArray() || rubric.isEmpty()) {
                    if (gradingMode == GradingMode.GRADED) {
                        issue(issues, "ERROR", "RUBRIC_REQUIRED", "sections[" + index + "].rubric",
                                "Mỗi câu được chấm điểm cần có tiêu chí và điểm cho từng tiêu chí.");
                    } else {
                        issue(issues, "WARNING", "RUBRIC_RECOMMENDED", "sections[" + index + "].rubric",
                                "Câu này chưa có rubric; có thể bổ sung trước khi dùng.");
                    }
                    continue;
                }
                for (int criterionIndex = 0; criterionIndex < rubric.size(); criterionIndex++) {
                    JsonNode criterion = rubric.get(criterionIndex);
                    JsonNode items = criterion.path("items");
                    String path = "sections[" + index + "].rubric[" + criterionIndex + "]";
                    if (criterion.path("title").asText("").isBlank() || !items.isArray() || items.isEmpty()) {
                        issue(issues, "ERROR", "RUBRIC_CRITERION_INVALID", path,
                                "Mỗi tiêu chí cần tên và ít nhất một mức điểm.");
                        continue;
                    }
                    for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                        JsonNode item = items.get(itemIndex);
                        String itemPath = path + ".items[" + itemIndex + "]";
                        JsonNode points = item.path("points");
                        if (item.path("text").asText("").isBlank() || !points.isNumber()) {
                            issue(issues, "ERROR", "RUBRIC_POINTS_REQUIRED", itemPath,
                                    "Mỗi mức chấm cần mô tả và số điểm.");
                            continue;
                        }
                        BigDecimal value = points.decimalValue();
                        if (value.signum() <= 0 || value.compareTo(AssignmentRubric.POINTS_MAX) > 0
                                || Math.max(0, value.stripTrailingZeros().scale()) > 2) {
                            issue(issues, "ERROR", "RUBRIC_POINTS_INVALID", itemPath + ".points",
                                    "Điểm phải lớn hơn 0, tối đa 1.000 và có không quá 2 chữ số thập phân.");
                            continue;
                        }
                        total = total.add(value).setScale(2);
                    }
                }
            }

            if (options.questionCount() != null && questionCount != options.questionCount()) {
                issue(issues, "ERROR", "QUESTION_COUNT_MISMATCH", "sections",
                        "Số câu cấp 1 không khớp với số câu đã chọn.");
            }
            if (options.targetPoints() != null && total.compareTo(options.targetPoints()) != 0) {
                issue(issues, "ERROR", "TOTAL_POINTS_MISMATCH", "sections.rubric",
                        "Tổng điểm rubric " + total.toPlainString() + " không bằng mục tiêu "
                                + options.targetPoints().toPlainString() + ".");
            }
        }

        boolean hasError = false;
        boolean hasWarning = false;
        for (JsonNode issue : issues) {
            hasError |= "ERROR".equals(issue.path("severity").asText());
            hasWarning |= "WARNING".equals(issue.path("severity").asText());
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("status", hasError ? "INVALID" : hasWarning ? "NEEDS_REVIEW" : "VALID");
        result.put("questionCount", questionCount);
        result.put("rubricLeafCount", leafCount);
        result.put("totalPoints", total);
        result.set("issues", issues);
        return result;
    }

    private static void issue(ArrayNode issues, String severity, String code, String path, String message) {
        ObjectNode issue = issues.addObject();
        issue.put("severity", severity);
        issue.put("code", code);
        issue.put("path", path);
        issue.put("message", message);
    }

    record Options(String profile, String language, Integer questionCount, BigDecimal targetPoints,
                   boolean requireTitle) {
        static Options legacy() {
            return new Options("GENERAL", "VI", null, null, false);
        }
    }
}
