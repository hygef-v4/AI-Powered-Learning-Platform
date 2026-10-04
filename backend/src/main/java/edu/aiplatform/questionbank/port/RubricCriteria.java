package edu.aiplatform.questionbank.port;

import java.math.BigDecimal;
import java.util.List;

/** Tiêu chí → mục checklist có điểm (BR-U06-30). */
public record RubricCriteria(List<Criterion> criteria) {
    public record Criterion(String id, String title, List<Item> items) {}

    public record Item(String id, String text, BigDecimal points) {}
}
