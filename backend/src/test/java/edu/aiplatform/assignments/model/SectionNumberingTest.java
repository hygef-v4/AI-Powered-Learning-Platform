package edu.aiplatform.assignments.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SectionNumberingTest {

    @Test
    void fourTiersUseRomanNumberLetterAndDoubledLetter() {
        assertThat(SectionNumbering.formatNumber(List.of(1))).isEqualTo("I.");
        assertThat(SectionNumbering.formatNumber(List.of(4, 2))).isEqualTo("IV.2.");
        assertThat(SectionNumbering.formatNumber(List.of(1, 1, 1))).isEqualTo("I.1.a.");
        assertThat(SectionNumbering.formatNumber(List.of(1, 1, 1, 1))).isEqualTo("I.1.a.aa.");
        assertThat(SectionNumbering.formatNumber(List.of(9, 3, 2, 3))).isEqualTo("IX.3.b.cc.");
    }

    @Test
    void lettersContinuePastZ() {
        assertThat(SectionNumbering.formatLetters(26)).isEqualTo("z");
        assertThat(SectionNumbering.formatLetters(27)).isEqualTo("aa");
    }
}
