package edu.aiplatform.assignments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.audit.contract.AuditResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** F15; BR-U07-53: người học đọc bài OPEN, CLOSED của lớp mình; giảng viên ghi đề xuất kèm expectedVersion. */
class AssignmentMcpToolsTest {

    private final AssignmentFixtures f = new AssignmentFixtures();
    private final AssignmentMcpTools tools = new AssignmentMcpTools(f.assignments, f.access, f.rubrics, f.documents,
            f.aiDrafts, f.classes, f.json);

    @Test
    @SuppressWarnings("unchecked")
    void studentReadsOpenAssignmentOfEnrolledClassOnly() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN,
                new DocumentModel(List.of(AssignmentFixtures.heading("H1", "Mục"))), true);
        Assignment draft = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        when(f.classes.listOpenClassesOf(f.student.accountId())).thenReturn(List.of(f.classRef(f.classId, ClassStatus.OPEN)));
        when(f.classes.isActiveStudent(f.student.accountId(), f.classId)).thenReturn(true);
        McpToolResult ok = tools.call(f.student, AssignmentMcpTools.READ, Map.of("assignmentId", open.id().toString()));
        assertThat(ok.error()).isFalse();
        assertThat((Map<String,Object>)ok.content()).containsKeys("assignmentId", "title", "status", "gradingMode")
                .doesNotContainKeys("instructions", "document", "parts", "rubrics");
        McpToolResult denied = tools.call(f.student, AssignmentMcpTools.READ, Map.of("assignmentId", draft.id().toString()));
        assertThat(denied.error()).isTrue();

    }

    @Test
    void saveDraftChecksExpectedVersionAndNeverEditsAssignment() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of()), false);
        Map<String, Object> draft = Map.of("instructions", "x", "sections", List.of(Map.of("level", 1, "title", "Mục A")));
        McpToolResult stale = tools.call(f.teacher, AssignmentMcpTools.SAVE, Map.of("assignmentId", a.id().toString(),
                "expectedVersion", a.version() + 1, "draft", draft));
        assertThat(stale.error()).isTrue();
        assertThat(stale.content()).asString().contains("ASSIGNMENT_VERSION_CONFLICT");
        McpToolResult saved = tools.call(f.teacher, AssignmentMcpTools.SAVE, Map.of("assignmentId", a.id().toString(),
                "expectedVersion", a.version(), "draft", draft));
        assertThat(saved.error()).isFalse();
        assertThat(f.store.get(a.id()).skeleton().blocks()).isEmpty();
    }
}
