package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.PromptAction;
import edu.aiplatform.ai.contract.PromptFlowState;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Máy trạng thái của quy trình dùng AI: chỉ những cặp (trạng thái, thao tác) có trong bảng mới được gửi; thiếu cặp là
 * từ chối. Ví dụ không sửa đề đã phát hành, không đổi barem đã khóa, sinh viên không hỏi AI khi chưa nộp bài.
 */
final class PromptFlowMachine {

    private static final Map<PromptFlowState, Map<PromptAction, PromptFlowState>> TRANSITIONS = buildTransitions();

    private PromptFlowMachine() {
    }

    private static Map<PromptFlowState, Map<PromptAction, PromptFlowState>> buildTransitions() {
        Map<PromptFlowState, Map<PromptAction, PromptFlowState>> table = new EnumMap<>(PromptFlowState.class);
        for (PromptFlowState state : PromptFlowState.values()) {
            table.put(state, new EnumMap<>(PromptAction.class));
        }
        // AI chỉ tạo bản đề xuất PENDING; nội dung chỉ đổi khi người dùng áp dụng nên trạng thái giữ nguyên.
        table.get(PromptFlowState.AUTHORING_DRAFT).put(PromptAction.CREATE_DRAFT, PromptFlowState.AUTHORING_DRAFT);
        table.get(PromptFlowState.AUTHORING_DRAFT).put(PromptAction.REVISE_DRAFT, PromptFlowState.AUTHORING_DRAFT);
        table.get(PromptFlowState.AUTHORING_DRAFT).put(PromptAction.GENERATE_QUESTIONS,
                PromptFlowState.AUTHORING_DRAFT);
        table.get(PromptFlowState.RUBRIC_EDITABLE).put(PromptAction.ADJUST_RUBRIC, PromptFlowState.RUBRIC_EDITABLE);
        table.get(PromptFlowState.ATTEMPT_SUBMITTED).put(PromptAction.EXPLAIN_FEEDBACK,
                PromptFlowState.ATTEMPT_SUBMITTED);
        table.get(PromptFlowState.OPEN).put(PromptAction.ASK, PromptFlowState.OPEN);
        table.replaceAll((state, actions) -> Collections.unmodifiableMap(actions));
        return Collections.unmodifiableMap(table);
    }

    /** Trạng thái sau thao tác; rỗng khi thao tác không hợp lệ ở trạng thái hiện tại. */
    static Optional<PromptFlowState> next(PromptFlowState state, PromptAction action) {
        if (state == null || action == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(TRANSITIONS.get(state).get(action));
    }
}
