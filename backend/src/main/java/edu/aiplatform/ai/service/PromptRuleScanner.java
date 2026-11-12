package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.GuardrailCategory;
import edu.aiplatform.ai.contract.GuardrailVerdict;
import edu.aiplatform.ai.contract.UserPromptInput;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tầng 1 — quy tắc, regex và heuristic, chạy trong bộ nhớ, không gọi dịch vụ ngoài: độ dài theo phạm vi, ký tự giấu
 * lệnh, mẫu prompt injection (Anh/Việt), nhập vai, dò system prompt, giả thẻ phân định, script/SQL payload và chuỗi
 * Base64/Hex dùng để né bộ lọc. Quy tắc so trên chữ đã chuẩn hóa ({@link PromptTextNormalizer}).
 */
final class PromptRuleScanner {

    static final int TIER = 1;

    record Rule(String id, GuardrailCategory category, Pattern pattern) {
    }

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE;
    private static final GuardrailCategory INJECTION = GuardrailCategory.PROMPT_INJECTION;
    private static final GuardrailCategory HARMFUL = GuardrailCategory.HARMFUL_MALICIOUS;

    static final List<Rule> RULES = List.of(
            // --- Ghi đè chỉ dẫn hệ thống ---
            rule("T1_OVERRIDE_EN", INJECTION, "\\b(ignore|disregard|forget|override|overrule)\\b[^.\\n]{0,40}?"
                    + "\\b(previous|prior|above|earlier|preceding|initial|original|all|any|every|system|your)\\b"
                    + "[^.\\n]{0,30}?\\b(instructions?|prompts?|rules?|directives?|guidelines?|constraints?"
                    + "|guardrails?|polic(?:y|ies))\\b"),
            rule("T1_BYPASS_EN", INJECTION, "\\b(bypass|circumvent|disable|deactivate|turn off|switch off|evade)\\b"
                    + "[^.\\n]{0,30}?\\b(rules?|filters?|safety|guardrails?|restrictions?|moderation"
                    + "|content polic(?:y|ies)|censorship|safeguards?)\\b"),
            rule("T1_UNBOUND_EN", INJECTION, "\\b(no longer (bound|restricted|limited) by|unrestricted mode"
                    + "|unfiltered mode|without any (rules|restrictions|filters))\\b"),
            rule("T1_OVERRIDE_VI", INJECTION, "\\b(bo qua|phot lo|lo di|quen( di| het)?|khong (can )?(tuan|lam) theo"
                    + "|vo hieu( hoa)?|vuot qua|lach|huy bo)\\b[^.\\n]{0,40}?\\b(chi dan|chi thi|quy tac|luat le"
                    + "|rang buoc|bo loc|kiem duyet|prompt|lenh he thong|cau lenh)\\s*(truoc( do)?|o tren|ban dau"
                    + "|he thong|cua (ban|he thong|ai)|duoc giao|an)\\b"),
            rule("T1_OVERRIDE_ALL_VI", INJECTION, "\\b(bo qua|phot lo|quen|khong tuan theo)\\b[^.\\n]{0,15}?"
                    + "\\b(moi|tat ca|toan bo) (cac )?(chi dan|chi thi|quy tac|rang buoc|bo loc|gioi han)\\b"),
            rule("T1_BYPASS_VI", INJECTION, "\\b(vuot|lach|vo hieu hoa|tat|go bo)\\b\\s*(qua\\s*)?"
                    + "(bo loc|kiem duyet|che do an toan|guardrail|gioi han an toan)\\b"),
            // --- Nhập vai nguy hiểm ---
            rule("T1_ROLEPLAY_EN", INJECTION, "\\b(you are now|from now on,? you (are|will be|will act)"
                    + "|pretend (to be|you are)|roleplay as|you will now act as)\\b"),
            rule("T1_ROLEPLAY_PERSONA_EN", INJECTION, "\\b(act as|behave as|become)\\s+(an? )?(unrestricted"
                    + "|unfiltered|uncensored|jailbroken|evil|admin|administrator|root|system|developer)\\b"),
            rule("T1_JAILBREAK_MODE", INJECTION, "\\b(do anything now|dan mode|developer mode|god mode|jailbroken"
                    + "|jailbreak mode|che do (nha phat trien|khong gioi han|khong kiem duyet|jailbreak))\\b"),
            rule("T1_ROLEPLAY_VI", INJECTION, "\\b(bay gio|tu gio( tro di)?|tu nay|ke tu bay gio|tu bay gio)\\b,?"
                    + "\\s*(ban|may|mi|em|ai)\\s*(la|se la|hay la|dong vai|tro thanh)\\b"),
            rule("T1_ROLEPLAY_PERSONA_VI", INJECTION, "\\b(dong vai|nhap vai|gia vo la|gia lam)\\b\\s*(mot\\s*)?"
                    + "(ai|mo hinh|tro ly|he thong)?\\s*(khong (bi )?(gioi han|kiem duyet|rang buoc)|hacker"
                    + "|admin|quan tri vien|nha phat trien|root)\\b"),
            // --- Dò system prompt ---
            rule("T1_SYSTEM_PROMPT_EN", INJECTION, "\\b(reveal|show|print|repeat|output|display|leak|dump|tell me"
                    + "|give me|what (is|are|was|were))\\b[^.\\n]{0,30}?\\b(system prompt|system message|hidden prompt"
                    + "|initial prompt|original prompt|developer (message|prompt)|your (instructions|prompt|rules"
                    + "|guidelines|configuration))\\b"),
            rule("T1_TEXT_ABOVE_EN", INJECTION, "\\b(repeat|print|output)\\b[^.\\n]{0,15}\\b(the )?(text|words"
                    + "|everything) (above|before)\\b"),
            rule("T1_SYSTEM_PROMPT_VI", INJECTION, "\\b(tiet lo|cho (toi|em|minh) (xem|biet)|in ra|hien thi|lap lai"
                    + "|nhac lai|doc lai|xuat ra|liet ke)\\b[^.\\n]{0,30}?\\b(system prompt|prompt (he thong|goc"
                    + "|ban dau|an)|chi dan (he thong|goc|ban dau|an)|chi thi (he thong|goc|an)|cau lenh (he thong"
                    + "|an)|huong dan (he thong|an|goc))\\b"),
            // --- Giả thẻ phân định / lượt hội thoại ---
            rule("T1_DELIMITER_SPOOF", INJECTION, "<\\s*/?\\s*(user_request|source_material|student_submission"
                    + "|current_document|bai_nop|untrusted_data|system|assistant|developer)\\s*>"),
            rule("T1_CHAT_TOKEN", INJECTION, "<\\|\\s*(im_start|im_end|system|endoftext|eot_id|start_header_id)"
                    + "\\s*\\|>|\\[/?\\s*(inst|sys)\\s*\\]|<<\\s*/?\\s*sys\\s*>>"),
            rule("T1_ROLE_HEADER", INJECTION, "^\\s*(#{2,}\\s*(system|instructions?)\\b|(system|assistant|developer)"
                    + "\\s*(prompt)?\\s*:)"),
            // --- Script / SQL payload ---
            rule("T1_SCRIPT", HARMFUL, "<\\s*/?\\s*script\\b|javascript\\s*:|\\bon(error|load|click|mouseover"
                    + "|focus)\\s*=|<\\s*(iframe|object|embed)\\b|data\\s*:\\s*text/html"),
            rule("T1_SQL_UNION", HARMFUL, "\\bunion\\s+(all\\s+)?select\\b"),
            rule("T1_SQL_STACKED", HARMFUL, ";\\s*(drop\\s+(table|database|schema)|delete\\s+from|truncate\\s+table"
                    + "|alter\\s+table|insert\\s+into|update\\s+\\w+\\s+set|exec(ute)?\\s+\\w)"),
            rule("T1_SQL_TAUTOLOGY", HARMFUL, "'\\s*(or|and)\\s+'?\\w+'?\\s*=\\s*'?\\w+|'\\s*;?\\s*--"),
            rule("T1_SQL_FUNCTION", HARMFUL, "\\b(xp_cmdshell|information_schema\\.|pg_sleep\\s*\\(|waitfor\\s+delay"
                    + "|benchmark\\s*\\(|sleep\\s*\\(\\s*\\d+\\s*\\))"));

    private static final Pattern BASE64_TOKEN = Pattern.compile("[A-Za-z0-9+/_-]{24,}={0,2}");
    private static final Pattern HEX_ESCAPES = Pattern.compile("(?:\\\\x[0-9a-fA-F]{2}){4,}");
    private static final Pattern HEX_RUN = Pattern.compile("\\b(?:0x)?((?:[0-9a-fA-F]{2}){12,})\\b");
    private static final int OPAQUE_BASE64_CHARS = 80;
    private static final int OPAQUE_HEX_ESCAPES = 8;

    private PromptRuleScanner() {
    }

    private static Rule rule(String id, GuardrailCategory category, String regex) {
        return new Rule(id, category, Pattern.compile(regex, FLAGS));
    }

    static Optional<GuardrailVerdict> scan(UserPromptInput input) {
        PromptContextPolicy policy = PromptContextPolicy.of(input.context());
        String raw = input.text() == null ? "" : input.text();
        if (PromptTextNormalizer.hasSmuggledCharacters(raw)) {
            return reject(INJECTION, "T1_HIDDEN_CHARACTERS", PromptContextPolicy.INJECTION_MESSAGE);
        }
        String cleaned = PromptTextNormalizer.clean(raw).strip();
        int length = PromptTextNormalizer.countCodePoints(cleaned);
        if (length > policy.maxChars()) {
            return reject(GuardrailCategory.OFF_TOPIC, "T1_LENGTH_MAX", policy.tooLongMessage());
        }
        if (input.action() != null && input.action().isTextRequired() && length < PromptContextPolicy.MIN_TEXT_CHARS) {
            return reject(GuardrailCategory.OFF_TOPIC, "T1_LENGTH_MIN", policy.tooShortMessage());
        }
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        Optional<Rule> hit = firstRule(cleaned);
        if (hit.isPresent()) {
            return reject(hit.get().category(), hit.get().id(), messageFor(hit.get().category()));
        }
        return scanEncoded(cleaned);
    }

    /** Quy tắc đầu tiên khớp một biến thể của chữ. */
    static Optional<Rule> firstRule(String cleaned) {
        for (String variant : PromptTextNormalizer.matchVariants(cleaned)) {
            for (Rule rule : RULES) {
                if (rule.pattern().matcher(variant).find()) {
                    return Optional.of(rule);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Base64/Hex: giải mã rồi quét lại bằng cùng bộ quy tắc; chuỗi mã hóa dài không đọc được là payload mờ, chặn vì
     * không có lý do để giảng viên/sinh viên gửi khối mã hóa dài cho AI.
     */
    private static Optional<GuardrailVerdict> scanEncoded(String cleaned) {
        Matcher base64 = BASE64_TOKEN.matcher(cleaned);
        while (base64.find()) {
            String token = base64.group();
            Optional<String> decoded = decodeBase64(token);
            if (decoded.isPresent() && firstRule(decoded.get()).isPresent()) {
                return reject(INJECTION, "T1_ENCODED_INJECTION", PromptContextPolicy.INJECTION_MESSAGE);
            }
            if (token.length() >= OPAQUE_BASE64_CHARS && looksEncoded(token)) {
                return reject(INJECTION, "T1_ENCODED_PAYLOAD", PromptContextPolicy.INJECTION_MESSAGE);
            }
        }
        Matcher escapes = HEX_ESCAPES.matcher(cleaned);
        while (escapes.find()) {
            String hex = escapes.group().replace("\\x", "").replace("\\X", "");
            Optional<String> decoded = decodeHex(hex);
            if (decoded.isPresent() && firstRule(decoded.get()).isPresent()) {
                return reject(INJECTION, "T1_ENCODED_INJECTION", PromptContextPolicy.INJECTION_MESSAGE);
            }
            if (hex.length() / 2 >= OPAQUE_HEX_ESCAPES) {
                return reject(INJECTION, "T1_ENCODED_PAYLOAD", PromptContextPolicy.INJECTION_MESSAGE);
            }
        }
        Matcher run = HEX_RUN.matcher(cleaned);
        while (run.find()) {
            Optional<String> decoded = decodeHex(run.group(1));
            if (decoded.isPresent() && firstRule(decoded.get()).isPresent()) {
                return reject(INJECTION, "T1_ENCODED_INJECTION", PromptContextPolicy.INJECTION_MESSAGE);
            }
        }
        return Optional.empty();
    }

    /** Chuỗi có cả chữ hoa, chữ thường và chữ số: dấu hiệu dữ liệu mã hóa thay vì một từ dài. */
    private static boolean looksEncoded(String token) {
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            upper |= Character.isUpperCase(c);
            lower |= Character.isLowerCase(c);
            digit |= Character.isDigit(c);
        }
        return upper && lower && digit;
    }

    static Optional<String> decodeBase64(String token) {
        String padded = token.replace('-', '+').replace('_', '/');
        int remainder = padded.replace("=", "").length() % 4;
        if (remainder == 1) {
            return Optional.empty();
        }
        String body = padded.replace("=", "") + "==".substring(0, remainder == 0 ? 0 : 4 - remainder);
        try {
            return printable(Base64.getDecoder().decode(body));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static Optional<String> decodeHex(String hex) {
        if (hex.length() % 2 != 0) {
            return Optional.empty();
        }
        try {
            return printable(HexFormat.of().parseHex(hex));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Chỉ coi là chữ khi ≥ 90% ký tự in được; tránh quét rác nhị phân. */
    private static Optional<String> printable(byte[] bytes) {
        if (bytes.length < 8) {
            return Optional.empty();
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        long readable = text.codePoints().filter(cp -> cp == '\n' || cp == '\t' || cp >= 0x20 && cp != 0xFFFD
                && !Character.isISOControl(cp)).count();
        return readable * 10 >= text.codePointCount(0, text.length()) * 9L ? Optional.of(text) : Optional.empty();
    }

    private static String messageFor(GuardrailCategory category) {
        return category == HARMFUL ? PromptContextPolicy.HARMFUL_MESSAGE : PromptContextPolicy.INJECTION_MESSAGE;
    }

    private static Optional<GuardrailVerdict> reject(GuardrailCategory category, String ruleId, String message) {
        return Optional.of(GuardrailVerdict.reject(category, TIER, ruleId, message));
    }
}
