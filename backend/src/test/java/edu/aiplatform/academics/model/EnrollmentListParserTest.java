package edu.aiplatform.academics.model;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EnrollmentListParserTest {
    @Test void keepsLineNumbersNormalizesAndMarksDuplicates() {
        var rows = EnrollmentListParser.parseEnrollmentRows("\uFEFFemail\r\n \"A@Example.com\" \r\n\r\na@example.com\r\ninvalid\r\nb@example.com,extra");
        assertThat(rows).hasSize(4);
        assertThat(rows.get(0).lineNumber()).isEqualTo(2);
        assertThat(rows.get(0).email()).isEqualTo("a@example.com");
        assertThat(rows.get(0).error()).isNull();
        assertThat(rows.get(1).error()).isEqualTo(EnrollmentResult.DUPLICATE_IN_LIST);
        assertThat(rows.get(2).error()).isEqualTo(EnrollmentResult.INVALID_IDENTIFIER);
        assertThat(rows.get(3).error()).isEqualTo(EnrollmentResult.INVALID_IDENTIFIER);
    }

    @Test void acceptsStudentCodesWithOptionalHeaderAndMarksInvalidIdentifiers() {
        var rows = EnrollmentListParser.parseEnrollmentRows("MSSV\n se000001 \nSE000001\na@example.com\nSE-00001\nAB12345");
        assertThat(rows).extracting(EnrollmentListParser.Row::identifier)
                .containsExactly("SE000001", "SE000001", "a@example.com", "se-00001", "ab12345");
        assertThat(rows.get(0).studentCode()).isEqualTo("SE000001");
        assertThat(rows.get(0).email()).isNull();
        assertThat(rows.get(0).lineNumber()).isEqualTo(2);
        assertThat(rows).extracting(EnrollmentListParser.Row::error).containsExactly(null,
                EnrollmentResult.DUPLICATE_IN_LIST, null, EnrollmentResult.INVALID_IDENTIFIER,
                EnrollmentResult.INVALID_IDENTIFIER);
        assertThat(EnrollmentListParser.parseEnrollmentRows("student_code\nSE000001")).hasSize(1);
    }

    @Test void enforcesRowsAndUtf8BytesBeforeLookup() {
        var twoHundred = IntStream.range(0, 200).mapToObj(i -> "s" + i + "@example.com").collect(Collectors.joining("\n"));
        assertThat(EnrollmentListParser.parseEnrollmentRows(twoHundred)).hasSize(200);
        assertThatThrownBy(() -> EnrollmentListParser.parseEnrollmentRows(twoHundred + "\nx@example.com"))
                .isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> EnrollmentListParser.parseEnrollmentRows("đ".repeat(51_201)))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("100 KB");
        assertThatThrownBy(() -> EnrollmentListParser.parseEnrollmentRows(" \r\n"))
                .isInstanceOf(AcademicRuleException.class);
    }

    @Test void rejectsBadQuotesWhitespaceAndOverlongAddresses() {
        var rows = EnrollmentListParser.parseEnrollmentRows("\"a@example.com\na b@example.com\na@@example.com\n" + "a".repeat(255) + "@example.com");
        assertThat(rows).allSatisfy(row -> assertThat(row.error()).isEqualTo(EnrollmentResult.INVALID_IDENTIFIER));
    }

    @Test void rejectsMalformedLocalPartDotsLikeTheUiPreview() {
        var rows = EnrollmentListParser.parseEnrollmentRows(".a@example.com\na.@example.com\na..b@example.com\na.b@example.com");
        assertThat(rows.subList(0, 3)).allSatisfy(row ->
                assertThat(row.error()).isEqualTo(EnrollmentResult.INVALID_IDENTIFIER));
        assertThat(rows.get(3).error()).isNull();
    }
}
