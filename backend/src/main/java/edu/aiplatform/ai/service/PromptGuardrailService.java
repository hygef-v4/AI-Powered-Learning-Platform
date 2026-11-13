package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.GuardrailCategory;
import edu.aiplatform.ai.contract.GuardrailVerdict;
import edu.aiplatform.ai.contract.IPromptGuardrail;
import edu.aiplatform.ai.contract.UserPromptInput;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pipeline guardrail tuần tự: Tầng 1 vi phạm thì từ chối ngay; chỉ yêu cầu qua Tầng 1 mới vào Tầng 2; Tầng 2 vi phạm
 * hoặc lỗi trong lúc kiểm thì từ chối (fail-closed). Chỉ verdict {@code VALID} mới được bên gọi gửi tới mô hình chính.
 * Log ghi phân loại, mã quy tắc, phạm vi và tài khoản; không ghi nội dung người dùng nhập.
 */
@Service
public class PromptGuardrailService implements IPromptGuardrail {

    private static final Logger LOG = LoggerFactory.getLogger(PromptGuardrailService.class);

    @Override
    public GuardrailVerdict evaluate(UserPromptInput input) {
        if (input == null || input.context() == null) {
            return failClosed(input, "T0_MISSING_INPUT", null);
        }
        GuardrailVerdict verdict;
        try {
            Optional<GuardrailVerdict> tierOne = PromptRuleScanner.scan(input);
            verdict = tierOne.isPresent() ? tierOne.get() : PromptPolicyEngine.evaluate(input);
        } catch (RuntimeException failure) {
            return failClosed(input, "T2_EVALUATION_ERROR", failure);
        }
        if (verdict == null) {
            return failClosed(input, "T2_NO_VERDICT", null);
        }
        if (!verdict.isAllowed()) {
            LOG.info("AI prompt rejected: tier={} category={} rule={} context={} action={} actor={}", verdict.tier(),
                    verdict.category(), verdict.ruleId(), input.context(), input.action(), actorId(input));
        }
        return verdict;
    }

    /** Lỗi trong lúc kiểm không bao giờ cho qua; coi như yêu cầu nguy hại để bên gọi không thử gửi tiếp. */
    private static GuardrailVerdict failClosed(UserPromptInput input, String ruleId, RuntimeException failure) {
        LOG.warn("AI prompt guardrail failed closed: rule={} context={} actor={} error={}", ruleId,
                input == null ? null : input.context(), actorId(input),
                failure == null ? "none" : failure.getClass().getSimpleName());
        return GuardrailVerdict.reject(GuardrailCategory.HARMFUL_MALICIOUS, PromptPolicyEngine.TIER, ruleId,
                PromptContextPolicy.UNAVAILABLE_MESSAGE);
    }

    private static Object actorId(UserPromptInput input) {
        return input == null || input.actor() == null ? null : input.actor().accountId();
    }
}
