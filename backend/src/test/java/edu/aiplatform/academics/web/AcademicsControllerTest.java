package edu.aiplatform.academics.web;

import edu.aiplatform.academics.application.*;
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
    AcademicsService service=mock(AcademicsService.class);
    Actor actor=new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER);
    MockMvc mvc;
    UUID id=UUID.randomUUID();
    @BeforeEach void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null));
        mvc=MockMvcBuilders.standaloneSetup(new AcademicsController(service)).setControllerAdvice(new AcademicsExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    @Test void listUsesServerPrincipalAndReturnsArray() throws Exception {
        when(service.list(actor,"manager",null,null,null,1,20)).thenReturn(List.of());
        mvc.perform(get("/api/v1/manager/classes")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).list(actor,"manager",null,null,null,1,20);
    }
    @Test void missingVersionIsBadRequest() throws Exception {
        mvc.perform(patch("/api/v1/manager/classes/"+id).contentType("application/json").content("{\"name\":\"New\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }
    @Test void cannotChangeImmutableFields() throws Exception {
        mvc.perform(patch("/api/v1/manager/classes/"+id).contentType("application/json").content("{\"name\":\"New\",\"version\":0,\"subjectId\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void conflictIncludesStudents() throws Exception {
        when(service.changeStatus(actor,id,edu.aiplatform.academics.contract.ClassStatus.OPEN,0)).thenThrow(new AcademicsProblem("ENROLLMENT_CONFLICT","Conflict",Map.of("conflictingStudents",List.of(id))));
        mvc.perform(put("/api/v1/manager/classes/"+id+"/status").contentType("application/json").content("{\"status\":\"OPEN\",\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.conflictingStudents[0]").value(id.toString()));
    }
    @Test void malformedEnumIsSafeBadRequest() throws Exception {
        mvc.perform(put("/api/v1/manager/classes/"+id+"/status").contentType("application/json").content("{\"status\":\"SECRET\",\"version\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
    @Test void downstreamUnavailableRemains503() throws Exception {
        when(service.detail(actor,"manager",id)).thenThrow(ApiException.unavailable("DEPENDENCY_UNAVAILABLE","Try later"));
        mvc.perform(get("/api/v1/manager/classes/"+id)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }
}
