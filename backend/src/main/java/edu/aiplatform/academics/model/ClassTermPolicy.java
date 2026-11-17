package edu.aiplatform.academics.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Học kỳ của lớp suy ra từ ngày bắt đầu (D-U04-05): tháng 1–4 {@code SP}, 5–8 {@code SU}, 9–12
 * {@code FA}, cộng hai số cuối của năm; lớp bắt đầu tháng 4, 8, 12 thêm {@code -BL5}. Cột
 * {@code course_classes.term} dùng cùng quy tắc nên hai nơi phải đổi cùng nhau.
 */
public final class ClassTermPolicy {
    public static final Pattern TERM_PATTERN = Pattern.compile("^(SP|SU|FA)[0-9]{2}(-BL5)?$");
    public static final LocalDate MIN_START = LocalDate.of(2000, 1, 1);
    public static final LocalDate MAX_START = LocalDate.of(2099, 12, 31);
    private static final String BLOCK_FIVE = "-BL5";
    private static final Comparator<String> NEWEST_FIRST = Comparator
            .comparingInt(ClassTermPolicy::parseTermYear)
            .thenComparingInt(ClassTermPolicy::rankTermSeason)
            .thenComparingInt(term -> term.endsWith(BLOCK_FIVE) ? 1 : 0)
            .reversed();

    private ClassTermPolicy() {
    }

    /** Calculates the term code of a class that starts on the given date. */
    public static String deriveTerm(LocalDate startsOn) {
        LocalDate start = requireValidStart(startsOn);
        int month = start.getMonthValue();
        String season = month <= 4 ? "SP" : month <= 8 ? "SU" : "FA";
        String year = String.format("%02d", start.getYear() % 100);
        return season + year + (month % 4 == 0 ? BLOCK_FIVE : "");
    }

    /** Requires a start date inside the supported range so the two-digit year is unambiguous. */
    public static LocalDate requireValidStart(LocalDate startsOn) {
        if (startsOn == null || startsOn.isBefore(MIN_START) || startsOn.isAfter(MAX_START)) {
            throw new AcademicRuleException("INVALID_CLASS_START_DATE",
                    "Ngày bắt đầu phải trong khoảng 2000–2099");
        }
        return startsOn;
    }

    /** Parses an optional term filter; blank means no filter and other values must match exactly. */
    public static String parseFilter(String term) {
        if (term == null || term.isBlank())
            return null;
        if (!TERM_PATTERN.matcher(term).matches())
            throw new AcademicRuleException("INVALID_CLASS_TERM", "Học kỳ không hợp lệ");
        return term;
    }

    /** Orders term codes from the newest to the oldest. */
    public static Comparator<String> compareNewestFirst() {
        return NEWEST_FIRST;
    }

    private static int parseTermYear(String term) {
        return Integer.parseInt(term.substring(2, 4));
    }

    private static int rankTermSeason(String term) {
        return switch (term.substring(0, 2)) {
        case "SP" -> 0;
        case "SU" -> 1;
        default -> 2;
        };
    }
}
