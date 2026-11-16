package edu.aiplatform.academics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClassTermPolicyTest {

    @ParameterizedTest
    @CsvSource({
        "2027-01-10,SP27", "2027-02-01,SP27", "2027-03-31,SP27", "2027-04-03,SP27-BL5",
        "2027-05-15,SU27", "2027-06-01,SU27", "2027-07-31,SU27", "2027-08-20,SU27-BL5",
        "2026-09-07,FA26", "2026-10-01,FA26", "2026-11-30,FA26", "2026-12-01,FA26-BL5",
        "2000-01-01,SP00", "2099-12-31,FA99-BL5", "2009-04-30,SP09-BL5"
    })
    void calculatesTermFromStartMonthAndYear(LocalDate startsOn, String expected) {
        assertThat(ClassTermPolicy.deriveTerm(startsOn)).isEqualTo(expected);
        assertThat(ClassTermPolicy.TERM_PATTERN.matcher(expected).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1999-12-31", "2100-01-01"})
    void rejectsStartDatesOutsideSupportedRange(LocalDate startsOn) {
        assertThatThrownBy(() -> ClassTermPolicy.deriveTerm(startsOn))
                .isInstanceOfSatisfying(AcademicRuleException.class,
                        error -> assertThat(error.readCode()).isEqualTo("INVALID_CLASS_START_DATE"));
    }

    @Test
    void rejectsMissingStartDate() {
        assertThatThrownBy(() -> ClassTermPolicy.requireValidStart(null))
                .isInstanceOfSatisfying(AcademicRuleException.class,
                        error -> assertThat(error.readCode()).isEqualTo("INVALID_CLASS_START_DATE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankFilterMeansNoFilter(String term) {
        assertThat(ClassTermPolicy.parseFilter(term)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SP27", "SU00", "FA99-BL5"})
    void acceptsExactTermFilters(String term) {
        assertThat(ClassTermPolicy.parseFilter(term)).isEqualTo(term);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sp27", "SP2027", "WI27", "SP27-BL4", " SP27", "SP27-bl5", "SP7"})
    void rejectsMalformedTermFilters(String term) {
        assertThatThrownBy(() -> ClassTermPolicy.parseFilter(term))
                .isInstanceOfSatisfying(AcademicRuleException.class,
                        error -> assertThat(error.readCode()).isEqualTo("INVALID_CLASS_TERM"));
    }

    @Test
    void ordersTermsNewestFirst() {
        var sorted = Stream.of("SP26", "FA26-BL5", "SU27", "SP27-BL5", "FA26", "SP27", "SU26-BL5")
                .sorted(ClassTermPolicy.compareNewestFirst()).toList();
        assertThat(sorted).isEqualTo(
                List.of("SU27", "SP27-BL5", "SP27", "FA26-BL5", "FA26", "SU26-BL5", "SP26"));
    }
}
