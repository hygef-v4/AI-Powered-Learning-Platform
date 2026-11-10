package edu.aiplatform.grading.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import edu.aiplatform.grading.dto.GradingViews.GradebookRow;
import edu.aiplatform.grading.dto.GradingViews.Person;
import edu.aiplatform.grading.service.GradebookExporter;
import edu.aiplatform.grading.service.GradebookService;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradebookControllerTest {
    @Test void delegatesStudentLookupOnceWithoutPagination() {
        var service = mock(GradebookService.class);
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var classId = UUID.randomUUID();
        var student = UUID.randomUUID();
        var row = new GradebookRow(new Person(student, "Student", "s@school.test"), List.of());
        when(service.loadStudentGrades(actor, classId, student)).thenReturn(row);
        assertThat(new GradebookController(service, mock(GradebookExporter.class))
                .loadStudentGrades(actor, classId, student)).isSameAs(row);
        verify(service).loadStudentGrades(actor, classId, student);
        verifyNoMoreInteractions(service);
    }
}
