package edu.aiplatform.grading.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.aiplatform.grading.service.GradeDistributionVisibilityService;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GradeDistributionVisibilityControllerTest {
    final GradeDistributionVisibilityService gradeDistributionVisibilityService = mock(GradeDistributionVisibilityService.class);
    final UUID classId = UUID.randomUUID(), assignmentId = UUID.randomUUID();
    final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    final String path = "/api/v1/classes/" + classId + "/assignments/" + assignmentId + "/grade-distribution-visibility";
    MockMvc mvc;

    @BeforeEach void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        mvc = MockMvcBuilders.standaloneSetup(new GradeDistributionVisibilityController(gradeDistributionVisibilityService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void getAndPutUseServerActorAndExposeVersionAndCanEdit() throws Exception {
        when(gradeDistributionVisibilityService.loadVisibility(actor, classId, assignmentId)).thenReturn(new GradeDistributionVisibilityService.View(false, 0, true));
        when(gradeDistributionVisibilityService.updateVisibility(actor, classId, assignmentId, true, 0)).thenReturn(new GradeDistributionVisibilityService.View(true, 1, true));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.canEdit").value(true));
        mvc.perform(put(path).contentType("application/json").content("{\"enabled\":true,\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.version").value(1));
        verify(gradeDistributionVisibilityService).updateVisibility(actor, classId, assignmentId, true, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"version\":0}", "{\"enabled\":true}", "{\"enabled\":true,\"version\":-1}"})
    void rejectsMissingOrNegativeFields(String body) throws Exception {
        mvc.perform(put(path).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(gradeDistributionVisibilityService);
    }
}
