package edu.aiplatform.quizzes.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dấu vân tay nội dung câu để chống trùng (BR-U10-08). Chuẩn hóa NFC, chữ thường, gộp khoảng trắng. Hash gồm nội dung và
 * các lựa chọn (không phụ thuộc thứ tự lựa chọn); không gồm giải thích, điểm, độ khó, thẻ. Câu viết lại khác hẳn từ ngữ
 * thì không phát hiện được; phần đó do người duyệt.
 */
public final class QuestionFingerprint {

    private QuestionFingerprint() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }

    public static String hash(QuestionContent content) {
        List<String> options = content.options().stream().map(o -> normalize(o.text())).sorted().toList();
        String joined = normalize(content.stem()) + "\u0000" + String.join("\u0000", options);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Hệ số Dice trên bộ ba ký tự của hai nội dung đã chuẩn hóa, từ 0 đến 1. */
    public static double similarity(String normalizedA, String normalizedB) {
        Set<String> a = trigrams(normalizedA);
        Set<String> b = trigrams(normalizedB);
        if (a.isEmpty() || b.isEmpty()) {
            return normalizedA.equals(normalizedB) ? 1 : 0;
        }
        int total = a.size() + b.size();
        a.retainAll(b);
        return 2.0 * a.size() / total;
    }

    private static Set<String> trigrams(String text) {
        Set<String> result = new HashSet<>();
        String padded = "  " + text + " ";
        for (int i = 0; i + 3 <= padded.length(); i++) {
            result.add(padded.substring(i, i + 3));
        }
        return result;
    }
}
