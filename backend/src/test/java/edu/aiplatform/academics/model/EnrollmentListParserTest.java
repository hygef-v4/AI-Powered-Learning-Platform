package edu.aiplatform.academics.model;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EnrollmentListParserTest {
    @Test void keepsLineNumbersNormalizesAndMarksDuplicates() {
        var rows = EnrollmentListParser.parse("\uFEFFemail\r\n \"A@Example.com\" \r\n\r\na@example.com\r\ninvalid\r\nb@example.com,extra");
        assertThat(rows).hasSize(4);
        assertThat(rows.get(0).lineNumber()).isEqualTo(2);
        assertThat(rows.get(0).email()).isEqualTo("a@example.com");
        assertThat(rows.get(0).error()).isNull();
        assertThat(rows.get(1).error()).isEqualTo(EnrollmentResult.DUPLICATE_IN_LIST);
        assertThat(rows.get(2).error()).isEqualTo(EnrollmentResult.INVALID_EMAIL);
        assertThat(rows.get(3).error()).isEqualTo(EnrollmentResult.INVALID_EMAIL);
    }

    @Test void enforcesRowsAndUtf8BytesBeforeLookup() {
        var twoHundred = IntStream.range(0, 200).mapToObj(i -> "s" + i + "@example.com").collect(Collectors.joining("\n"));
        assertThat(EnrollmentListParser.parse(twoHundred)).hasSize(200);
        assertThatThrownBy(() -> EnrollmentListParser.parse(twoHundred + "\nx@example.com"))
                .isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> EnrollmentListParser.parse("đ".repeat(51_201)))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("100 KB");
        assertThatThrownBy(() -> EnrollmentListParser.parse(" \r\n"))
                .isInstanceOf(AcademicRuleException.class);
    }

    @Test void rejectsBadQuotesWhitespaceAndOverlongAddresses() {
        var rows = EnrollmentListParser.parse("\"a@example.com\na b@example.com\na@@example.com\n" + "a".repeat(255) + "@example.com");
        assertThat(rows).allSatisfy(row -> assertThat(row.error()).isEqualTo(EnrollmentResult.INVALID_EMAIL));
    }

    @Test void rejectsMalformedLocalPartDotsLikeTheUiPreview() {
        var rows = EnrollmentListParser.parse(".a@example.com\na.@example.com\na..b@example.com\na.b@example.com");
        assertThat(rows.subList(0, 3)).allSatisfy(row ->
                assertThat(row.error()).isEqualTo(EnrollmentResult.INVALID_EMAIL));
        assertThat(rows.get(3).error()).isNull();
    }
}
