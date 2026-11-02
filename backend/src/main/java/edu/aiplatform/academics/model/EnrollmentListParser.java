package edu.aiplatform.academics.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** CSV một cột, header email tùy chọn; không tra tài khoản và không tạo ghi danh. */
public final class EnrollmentListParser {
    public static final int MAX_ROWS = 200;
    public static final int MAX_BYTES = 100 * 1024;
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");

    private EnrollmentListParser() { }

    public record Row(int lineNumber, String email, EnrollmentResult error) { }

    public static List<Row> parse(String input) {
        if (input == null || input.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new AcademicRuleException("LIST_TOO_LARGE", "Danh sách tối đa 100 KB");
        }
        String text = input.startsWith("\uFEFF") ? input.substring(1) : input;
        var rows = new ArrayList<Row>();
        var seen = new HashSet<String>();
        String[] lines = text.split("\\r\\n|\\n|\\r", -1);
        boolean first = true;
        for (int i = 0; i < lines.length; i++) {
            String value = lines[i].strip();
            if (value.isEmpty()) continue;
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1).replace("\"\"", "\"").strip();
            }
            if (first && value.equalsIgnoreCase("email")) { first = false; continue; }
            first = false;
            if (rows.size() == MAX_ROWS) throw new AcademicRuleException("TOO_MANY_ROWS", "Danh sách tối đa 200 dòng");
            String email = value.toLowerCase(Locale.ROOT);
            EnrollmentResult error = email.length() > 254 || !EMAIL.matcher(email).matches() ? EnrollmentResult.INVALID_EMAIL :
                    !seen.add(email) ? EnrollmentResult.DUPLICATE_IN_LIST : null;
            rows.add(new Row(i + 1, email, error));
        }
        if (rows.isEmpty()) throw new AcademicRuleException("EMPTY_LIST", "Danh sách cần ít nhất một email");
        return List.copyOf(rows);
    }
}
