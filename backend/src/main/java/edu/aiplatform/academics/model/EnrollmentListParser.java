package edu.aiplatform.academics.model;

import edu.aiplatform.identity.contract.StudentCodeFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * CSV một cột, mỗi dòng một email hoặc một MSSV, header {@code email}/{@code student_code}/{@code mssv} tùy chọn
 * (BR-U05-56, 57); không tra tài khoản và không tạo ghi danh.
 */
public final class EnrollmentListParser {
    public static final int MAX_ROWS = 200;
    public static final int MAX_BYTES = 100 * 1024;
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");
    private static final Set<String> HEADERS = Set.of("email", "student_code", "mssv");

    /** Prevents instantiation of this static enrollment parser. */
    private EnrollmentListParser() {
    }

    /**
     * Một dòng đã chuẩn hóa: dòng hợp lệ có đúng một trong {@code email} hoặc {@code studentCode}; dòng sai định dạng
     * giữ giá trị đã nhập (chữ thường) ở {@code email}.
     */
    public record Row(int lineNumber, String email, String studentCode, EnrollmentResult error) {

        public Row(int lineNumber, String email, EnrollmentResult error) {
            this(lineNumber, email, null, error);
        }

        /** Giá trị người dùng đã nhập, hiện lại trong kết quả (BR-U05-58). */
        public String identifier() {
            return studentCode != null ? studentCode : email;
        }
    }

    /**
     * Parses a single-column CSV of email addresses or student codes, validating syntax, UTF-8 size, and in-list
     * duplicates.
     */
    public static List<Row> parseEnrollmentRows(String input) {
        if (input == null || input.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new AcademicRuleException("LIST_TOO_LARGE", "Danh sách tối đa 100 KB");
        }
        String text = input.startsWith("﻿") ? input.substring(1) : input;
        var rows = new ArrayList<Row>();
        var seen = new HashSet<String>();
        String[] lines = text.split("\\r\\n|\\n|\\r", -1);
        boolean first = true;
        for (int i = 0; i < lines.length; i++) {
            String value = lines[i].strip();
            if (value.isEmpty())
                continue;
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1).replace("\"\"", "\"").strip();
            }
            if (first && HEADERS.contains(value.toLowerCase(Locale.ROOT))) {
                first = false;
                continue;
            }
            first = false;
            if (rows.size() == MAX_ROWS)
                throw new AcademicRuleException("TOO_MANY_ROWS", "Danh sách tối đa 200 dòng");
            rows.add(parseRow(i + 1, value, seen));
        }
        if (rows.isEmpty())
            throw new AcademicRuleException("EMPTY_LIST", "Danh sách cần ít nhất một email hoặc MSSV");
        return List.copyOf(rows);
    }

    /** Dòng có {@code @} là email; còn lại khớp định dạng MSSV thì là MSSV (BR-U05-56). */
    private static Row parseRow(int lineNumber, String value, Set<String> seen) {
        if (!value.contains("@") && StudentCodeFormat.isValid(StudentCodeFormat.normalize(value))) {
            String code = StudentCodeFormat.normalize(value);
            return new Row(lineNumber, null, code, seen.add(code) ? null : EnrollmentResult.DUPLICATE_IN_LIST);
        }
        String email = value.toLowerCase(Locale.ROOT);
        EnrollmentResult error = !email.contains("@") || email.length() > 254 || !EMAIL.matcher(email).matches()
                ? EnrollmentResult.INVALID_IDENTIFIER
                : !seen.add(email) ? EnrollmentResult.DUPLICATE_IN_LIST : null;
        return new Row(lineNumber, email, null, error);
    }
}
