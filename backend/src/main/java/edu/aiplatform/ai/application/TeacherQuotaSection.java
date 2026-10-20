package edu.aiplatform.ai.application;

import edu.aiplatform.ai.domain.QuotaPolicy;
import edu.aiplatform.ai.domain.QuotaRepository;
import edu.aiplatform.settings.contract.ISettingsSectionProvider;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Thẻ "Quota AI của giảng viên" trên Setting List (F14): mức mặc định, trần ngày, số giảng viên có mức riêng. */
@Component
public class TeacherQuotaSection implements ISettingsSectionProvider {

    public static final String KEY = "teacher-quota";

    private final QuotaRepository quota;

    public TeacherQuotaSection(QuotaRepository quota) {
        this.quota = quota;
    }

    @Override
    public String sectionKey() {
        return KEY;
    }

    @Override
    public String title() {
        return "Quota AI của giảng viên";
    }

    @Override
    public String summary() {
        QuotaPolicy policy = quota.policy();
        if (!policy.configured()) {
            return "Chưa cấu hình";
        }
        int custom = quota.listTeacherQuotas().size();
        return "Mặc định " + tokens(policy.defaultDayTokens()) + " token/ngày · trần chung "
                + tokens(policy.globalDayTokens()) + " token/ngày · " + custom + " giảng viên có mức riêng";
    }

    static String tokens(long value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }
}
