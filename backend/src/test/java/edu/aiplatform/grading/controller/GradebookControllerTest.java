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
    @Test
    void exportUsesUtf8FilenameAndStreamsBothFormats() throws Exception {
        var service = mock(GradebookService.class);
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var classId = UUID.randomUUID();
        var assignmentId = UUID.randomUUID();
        for (String format : List.of("csv", "xlsx")) {
            when(service.buildExport(actor, classId, assignmentId, format)).thenReturn(
                    new GradebookService.GradebookExport(List.of(List.of("Student")), "Bài tập tuần 1"));
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            new GradebookController(service, new GradebookExporter())
                    .exportGrades(actor, classId, assignmentId, format, response);
            assertThat(org.springframework.http.ContentDisposition.parse(
                    response.getHeader("Content-Disposition")).getFilename())
                    .isEqualTo("Bài tập tuần 1." + format);
            assertThat(response.getHeader("Content-Disposition")).contains("filename*=UTF-8''");
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
            assertThat(response.getContentAsByteArray()).isNotEmpty();
        }
        when(service.buildExport(actor, classId, null, "xlsx")).thenReturn(
                new GradebookService.GradebookExport(List.of(List.of("Student")), null));
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        new GradebookController(service, new GradebookExporter())
                .exportGrades(actor, classId, null, "xlsx", response);
        assertThat(org.springframework.http.ContentDisposition.parse(
                response.getHeader("Content-Disposition")).getFilename())
                .startsWith("Bảng điểm tổng hợp-").endsWith(".xlsx").doesNotContain(classId.toString());
    }

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
