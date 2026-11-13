package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.GuardrailCategory;
import edu.aiplatform.ai.contract.GuardrailVerdict;
import edu.aiplatform.ai.contract.UserPromptInput;
import edu.aiplatform.ai.service.PromptLexicon.PhraseSet;
import edu.aiplatform.ai.service.PromptTextAnalysis.Tokens;
import java.util.List;
import java.util.Optional;

/**
 * Tầng 2 — đánh giá logic xác định, không regex, không AI hay API ngoài. Thứ tự: thao tác khớp phạm vi, RBAC, máy
 * trạng thái, rồi phân tích nội dung (tách trích dẫn khỏi chỉ dẫn): tự nâng quyền, câu ra lệnh cho hệ thống AI, nội
 * dung gây hại, ý định bị cấm của phạm vi và lạc đề. Không lặp lại mẫu regex của Tầng 1.
 */
final class PromptPolicyEngine {

    static final int TIER = 2;

    private PromptPolicyEngine() {
    }

    static GuardrailVerdict evaluate(UserPromptInput input) {
        if (input.actor() == null || input.actor().role() == null || input.context() == null
                || input.action() == null || input.state() == null) {
            return reject(GuardrailCategory.HARMFUL_MALICIOUS, "T2_INCOMPLETE_INPUT",
                    PromptContextPolicy.UNAVAILABLE_MESSAGE);
        }
        PromptContextPolicy policy = PromptContextPolicy.of(input.context());
        if (input.action().context() != input.context()) {
            return reject(GuardrailCategory.OFF_TOPIC, "T2_ACTION_CONTEXT", policy.offTopicMessage());
        }
        if (!policy.allows(input.actor().role())) {
            return reject(GuardrailCategory.HARMFUL_MALICIOUS, "T2_RBAC_ROLE", PromptContextPolicy.PERMISSION_MESSAGE);
        }
        if (PromptFlowMachine.next(input.state(), input.action()).isEmpty()) {
            return reject(GuardrailCategory.OFF_TOPIC, "T2_FLOW_STATE", PromptContextPolicy.STATE_MESSAGE);
        }
        String text = PromptTextNormalizer.clean(input.text()).strip();
        if (text.isEmpty()) {
            return GuardrailVerdict.valid();
        }
        return evaluateContent(input, policy, text).orElse(GuardrailVerdict.valid());
    }

    private static Optional<GuardrailVerdict> evaluateContent(UserPromptInput input, PromptContextPolicy policy,
                                                              String text) {
        List<PromptTextAnalysis.Segment> segments = PromptTextAnalysis.segments(text);
        String directive = PromptTextAnalysis.directiveText(segments);
        Tokens directiveTokens = PromptTextAnalysis.tokenize(directive);
        Tokens allTokens = PromptTextAnalysis.tokenize(text);

        if (PromptLexicon.claimsHigherRole(input.actor().role(), directiveTokens)) {
            return rejectContent(GuardrailCategory.PROMPT_INJECTION, "T2_PRIVILEGE_ESCALATION",
                    PromptContextPolicy.INJECTION_MESSAGE);
        }
        for (String sentence : PromptTextAnalysis.sentences(directive)) {
            if (targetsSystem(PromptTextAnalysis.tokenize(sentence))) {
                return rejectContent(GuardrailCategory.PROMPT_INJECTION, "T2_SYSTEM_DIRECTIVE",
                        PromptContextPolicy.INJECTION_MESSAGE);
            }
        }
        if (PromptLexicon.ALWAYS_HARMFUL.matches(allTokens)) {
            return rejectContent(GuardrailCategory.HARMFUL_MALICIOUS, "T2_HARMFUL_CONTENT",
                    PromptContextPolicy.HARMFUL_MESSAGE);
        }
        for (String sentence : PromptTextAnalysis.sentences(text)) {
            if (requestsHarm(PromptTextAnalysis.tokenize(sentence))) {
                return rejectContent(GuardrailCategory.HARMFUL_MALICIOUS, "T2_HARMFUL_REQUEST",
                        PromptContextPolicy.HARMFUL_MESSAGE);
            }
        }
        if (PromptLexicon.forbiddenIntentOf(input.context()).matches(directiveTokens)) {
            return rejectContent(GuardrailCategory.OFF_TOPIC, "T2_FORBIDDEN_INTENT", policy.offTopicMessage());
        }
        PhraseSet scope = PromptLexicon.scopeOf(input.context());
        boolean offTopicSignal = PromptLexicon.OFF_TOPIC.matches(allTokens);
        if (scope.count(allTokens) == 0 && (offTopicSignal || PromptLexicon.requiresScopeSignal(input.context()))) {
            return rejectContent(GuardrailCategory.OFF_TOPIC, "T2_OFF_TOPIC", policy.offTopicMessage());
        }
        return Optional.empty();
    }

    /**
     * Một câu chỉ dẫn ra lệnh cho chính hệ thống AI: có động từ điều khiển, và đối tượng là chỉ dẫn của AI (kèm nhắc AI
     * hoặc từ chỉ định "trước đó/của bạn"), hoặc đối tượng nghiệp vụ nhưng có từ chỉ định hệ thống.
     */
    static boolean targetsSystem(Tokens sentence) {
        if (!PromptLexicon.CONTROL_VERB.matches(sentence)) {
            return false;
        }
        boolean qualified = PromptLexicon.SYSTEM_QUALIFIER.matches(sentence);
        if (PromptLexicon.STRONG_TARGET.matches(sentence)) {
            return qualified || PromptLexicon.AI_REFERENCE.matches(sentence);
        }
        return qualified && PromptLexicon.WEAK_TARGET.matches(sentence);
    }

    /**
     * Yêu cầu tạo/vận hành thứ gây hại trong cùng một câu; khung câu hỏi/đề bài làm nó thành nội dung giảng dạy, trừ khi
     * câu đòi chi tiết vận hành (mã chạy được, từng bước).
     */
    static boolean requestsHarm(Tokens sentence) {
        if (!PromptLexicon.HARM_OBJECT.matches(sentence) || !PromptLexicon.HARM_ACTION.matches(sentence)) {
            return false;
        }
        return PromptLexicon.OPERATIONAL_DETAIL.matches(sentence) || !PromptLexicon.ASSESSMENT_FRAME.matches(sentence);
    }

    private static GuardrailVerdict reject(GuardrailCategory category, String ruleId, String message) {
        return GuardrailVerdict.reject(category, TIER, ruleId, message);
    }

    private static Optional<GuardrailVerdict> rejectContent(GuardrailCategory category, String ruleId,
                                                            String message) {
        return Optional.of(reject(category, ruleId, message));
    }
}
