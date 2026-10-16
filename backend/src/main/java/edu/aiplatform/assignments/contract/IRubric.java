package edu.aiplatform.assignments.contract;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Đọc rubric đã khóa, tính điểm theo mục đạt. Dùng bởi U08, U09, U12. */
public interface IRubric {

    List<RubricView> listRubrics(UUID assignmentId);

    RubricScore score(UUID rubricId, Set<UUID> checkedItemIds);

    BigDecimal maxPoints(UUID assignmentId);
}
