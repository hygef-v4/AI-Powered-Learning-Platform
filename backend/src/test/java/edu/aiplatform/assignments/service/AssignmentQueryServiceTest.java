package edu.aiplatform.assignments.service;

import static edu.aiplatform.assignments.service.AssignmentFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.model.Assignment;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F16; BR-U07-34: đúng hạn tới closesAt, trễ tới lateUntil, sau đó hoặc chưa mở là đóng. */
class AssignmentQueryServiceTest {

    private final AssignmentFixtures f = new AssignmentFixtures();

    @Test
    void submissionWindowFollowsSchedule() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        Assignment late = a.withSchedule(AssignmentStatus.OPEN, NOW.minus(Duration.ofDays(2)), NOW.plus(Duration.ofDays(1)),
                NOW.plus(Duration.ofDays(3)), 1, NOW);
        assertThat(AssignmentQueryService.window(late, NOW)).isEqualTo(SubmissionWindow.ON_TIME);
        assertThat(AssignmentQueryService.window(late, NOW.plus(Duration.ofDays(2)))).isEqualTo(SubmissionWindow.LATE);
        assertThat(AssignmentQueryService.window(late, NOW.plus(Duration.ofDays(4)))).isEqualTo(SubmissionWindow.CLOSED);
        assertThat(AssignmentQueryService.window(late.withStatus(AssignmentStatus.RETIRED, NOW), NOW))
                .isEqualTo(SubmissionWindow.CLOSED);
        assertThat(AssignmentQueryService.window(late, NOW.minus(Duration.ofDays(3)))).isEqualTo(SubmissionWindow.CLOSED);
    }

    @Test
    void contentIncludesRubricsInPartOrder() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN,
                new DocumentModel(List.of(AssignmentFixtures.heading("H1", "A"), AssignmentFixtures.heading("H1", "B"))), true);
        AssignmentQueryService query = new AssignmentQueryService(f.assignments, f.rubrics, f.classes);
        assertThat(query.getContent(a.id())).hasValueSatisfying(c -> {
            assertThat(c.parts()).hasSize(2);
            assertThat(c.rubrics()).extracting(r -> r.partId()).containsExactly(a.parts().get(0).partId(), a.parts().get(1).partId());
        });
        assertThat(f.rubrics.maxPoints(a.id())).isEqualByComparingTo("10.00");
    }
}
