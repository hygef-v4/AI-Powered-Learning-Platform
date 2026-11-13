package edu.aiplatform.assignments.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SectionNumberingTest {

    @Test
    void fourTiersUseRomanNumberLetterAndDoubledLetter() {
        assertThat(SectionNumbering.of(List.of(1))).isEqualTo("I.");
        assertThat(SectionNumbering.of(List.of(4, 2))).isEqualTo("IV.2.");
        assertThat(SectionNumbering.of(List.of(1, 1, 1))).isEqualTo("I.1.a.");
        assertThat(SectionNumbering.of(List.of(1, 1, 1, 1))).isEqualTo("I.1.a.aa.");
        assertThat(SectionNumbering.of(List.of(9, 3, 2, 3))).isEqualTo("IX.3.b.cc.");
    }

    @Test
    void lettersContinuePastZ() {
        assertThat(SectionNumbering.letters(26)).isEqualTo("z");
        assertThat(SectionNumbering.letters(27)).isEqualTo("aa");
    }
}
