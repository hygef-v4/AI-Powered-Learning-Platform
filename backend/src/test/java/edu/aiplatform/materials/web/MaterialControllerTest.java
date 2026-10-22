package edu.aiplatform.materials.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.application.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

class MaterialControllerTest {
    MaterialService commands;MaterialQueryService queries;MockMvc mvc;
    final UUID id=UUID.randomUUID();final Actor actor=new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER);
    @BeforeEach void setup() {
        commands=mock(MaterialService.class);queries=mock(MaterialQueryService.class);
        mvc=MockMvcBuilders.standaloneSetup(new MaterialController(commands,queries),new SubjectMaterialController(commands,queries),
                new ClassMaterialController(commands,queries),new MyMaterialController(queries))
                .setControllerAdvice(new MaterialExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @Test void usesServerPrincipalAndPreservesClassContext() throws Exception {
        UUID classId=UUID.randomUUID();mvc.perform(get("/api/v1/materials/"+id+"/view").param("classId",classId.toString())).andExpect(status().isOk());
        verify(queries).detail(actor,id,classId);
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"title\":\"\",\"version\":0}","{\"title\":\"A\",\"version\":-1}","{\"title\":\"A\",\"version\":null}"})
    void updateRejectsInvalidInputBeforeService(String json) throws Exception {
        mvc.perform(patch("/api/v1/materials/"+id).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        verifyNoInteractions(commands,queries);
    }
    @Test void createValidatesKindAndNestedSource() throws Exception {
        mvc.perform(post("/api/v1/subjects/"+id+"/materials").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"FILE\",\"title\":\"A\"}")).andExpect(status().isBadRequest());verifyNoInteractions(commands);
    }
    @Test void emptyReorderRejected() throws Exception {
        mvc.perform(put("/api/v1/subjects/"+id+"/materials:reorder").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(commands);
    }
    @Test void archiveRequiresVersion() throws Exception {
        mvc.perform(post("/api/v1/materials/"+id+"/archive").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(commands);
    }
    @Test void deniedDownloadIs404AndSafe() throws Exception {
        when(queries.download(actor,id,null,null)).thenThrow(edu.aiplatform.shared.error.ApiException.notFound("MATERIAL_NOT_FOUND"));
        mvc.perform(post("/api/v1/materials/"+id+"/download-token")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MATERIAL_NOT_FOUND"));
    }
    @Test void unexpectedProviderErrorDoesNotLeakDetails() throws Exception {
        when(queries.detail(actor,id,null)).thenThrow(new IllegalStateException("jdbc://secret"));
        mvc.perform(get("/api/v1/materials/"+id)).andExpect(status().isServiceUnavailable()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }
    @Test void learnerAttachedIdsPassToScopedQuery() throws Exception {
        UUID materialId=UUID.randomUUID();mvc.perform(get("/api/v1/me/classes/"+id+"/materials").param("ids",materialId.toString())).andExpect(status().isOk());
        verify(queries).classList(actor,id,true,List.of(materialId));
    }
}
