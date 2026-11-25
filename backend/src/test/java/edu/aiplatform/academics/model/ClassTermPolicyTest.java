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
    @ValueSource(strings = {"sp27", "SP2027", "WI27", "SP27-BL4", " SP27", "SP27-bl5", "SP7"})
    void rejectsMalformedTermFilters(String term) {
        assertThatThrownBy(() -> ClassTermPolicy.parseFilter(term))
                .isInstanceOfSatisfying(AcademicRuleException.class,
                        error -> assertThat(error.readCode()).isEqualTo("INVALID_CLASS_TERM"));
    }
}
