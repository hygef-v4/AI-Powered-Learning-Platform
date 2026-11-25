package edu.aiplatform.academics.controller;


import edu.aiplatform.academics.service.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicsControllerTest {
    AcademicsService academicsService=mock(AcademicsService.class);
    Actor actor=new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER);
    MockMvc mvc;
    UUID id=UUID.randomUUID();
    @BeforeEach void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null));
        mvc=MockMvcBuilders.standaloneSetup(new AcademicsController(academicsService)).setControllerAdvice(new AcademicsExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    @Test void conflictIncludesStudents() throws Exception {
        when(academicsService.changeClassStatus(actor,id,edu.aiplatform.academics.contract.ClassStatus.OPEN,0)).thenThrow(new AcademicsProblem("ENROLLMENT_CONFLICT","Conflict",Map.of("conflictingStudents",List.of(id))));
        mvc.perform(put("/api/v1/manager/classes/"+id+"/status").contentType("application/json").content("{\"status\":\"OPEN\",\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.conflictingStudents[0]").value(id.toString()));
    }
    @Test void downstreamUnavailableRemains503() throws Exception {
        when(academicsService.loadClass(actor,"manager",id)).thenThrow(ApiException.createUnavailable("DEPENDENCY_UNAVAILABLE","Try later"));
        mvc.perform(get("/api/v1/manager/classes/"+id)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }
}
