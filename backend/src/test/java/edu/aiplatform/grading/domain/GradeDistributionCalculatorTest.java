package edu.aiplatform.grading.domain;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class GradeDistributionCalculatorTest {
    @Test
    void suppressesSmallCohortsAndSmallBuckets() {
        var c = new GradeDistributionCalculator();
        assertThat(c.calculate(Collections.nCopies(19, BigDecimal.TEN), Collections.nCopies(19, BigDecimal.TEN)))
                .isEmpty();
        var scores = new ArrayList<>(Collections.nCopies(16, BigDecimal.TEN));
        scores.addAll(Collections.nCopies(4, BigDecimal.ZERO));
        var bins = c.calculate(scores, Collections.nCopies(20, BigDecimal.TEN));
        assertThat(bins).hasSize(1);
        assertThat(bins.get(0).count()).isEqualTo(16);
        assertThat(bins.get(0).label()).isEqualTo("80–100%");
    }

    @Test
    void excludesZeroMaximumRatherThanDividingByZero() {
        var c = new GradeDistributionCalculator();
        assertThat(c.calculate(Collections.nCopies(20, BigDecimal.ZERO), Collections.nCopies(20, BigDecimal.ZERO)))
                .isEmpty();
    }
}
