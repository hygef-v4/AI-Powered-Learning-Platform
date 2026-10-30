package edu.aiplatform.grading.model;

import java.math.*;
import java.util.ArrayList;
import java.util.List;

public class GradeDistributionCalculator {
    public record Bucket(String label, int count) {
    }

    public List<Bucket> calculate(List<BigDecimal> scores, List<BigDecimal> maxScores) {
        int[] counts = new int[5];
        int n = 0;
        for (int i = 0; i < scores.size(); i++)
            if (scores.get(i) != null && maxScores.get(i) != null && maxScores.get(i).signum() > 0) {
                int bin = scores.get(i).multiply(BigDecimal.valueOf(5)).divide(maxScores.get(i), 0, RoundingMode.DOWN)
                        .intValue();
                counts[Math.min(4, Math.max(0, bin))]++;
                n++;
            }
        if (n < 20)
            return List.of();
        List<Bucket> result = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            if (counts[i] >= 5)
                result.add(new Bucket((i * 20) + "–" + ((i + 1) * 20) + "%", counts[i]));
        return List.copyOf(result);
    }
}
