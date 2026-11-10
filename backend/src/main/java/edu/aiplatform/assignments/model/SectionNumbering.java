package edu.aiplatform.assignments.model;

import java.util.List;

/**
 * Số thứ tự câu hỏi như trình soạn hiển thị (yêu cầu 2026-11-10): tầng 1 số La Mã, tầng 2 số, tầng 3 chữ thường, tầng 4
 * chữ thường gấp đôi, ví dụ "I.", "I.1.", "I.1.a.", "I.1.a.aa.". Tối đa 4 tầng.
 */
public final class SectionNumbering {

    /** Số tầng câu hỏi tối đa. */
    public static final int MAX_DEPTH = 4;

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private SectionNumbering() {
    }

    /** {@code counters}: thứ tự (từ 1) của câu hỏi ở từng tầng, từ ngoài vào trong. */
    public static String of(List<Integer> counters) {
        StringBuilder out = new StringBuilder();
        for (int depth = 0; depth < counters.size(); depth++) {
            out.append(part(depth, Math.max(1, counters.get(depth)))).append('.');
        }
        return out.toString();
    }

    static String part(int depth, int n) {
        return switch (depth) {
            case 0 -> roman(n);
            case 1 -> String.valueOf(n);
            case 2 -> letters(n);
            default -> letters(n).repeat(2);
        };
    }

    static String roman(int n) {
        StringBuilder out = new StringBuilder();
        int rest = n;
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (rest >= ROMAN_VALUES[i]) {
                out.append(ROMAN_SYMBOLS[i]);
                rest -= ROMAN_VALUES[i];
            }
        }
        return out.toString();
    }

    /** a, b, …, z, aa, ab, … (cơ số 26 không có số 0). */
    static String letters(int n) {
        StringBuilder out = new StringBuilder();
        int rest = n;
        while (rest > 0) {
            rest--;
            out.insert(0, (char) ('a' + rest % 26));
            rest /= 26;
        }
        return out.toString();
    }
}
