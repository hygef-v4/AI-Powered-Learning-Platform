package edu.aiplatform.grading.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.grading.dto.GradingViews.BatchResult;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.service.GradingCommands.AiBatch;
import edu.aiplatform.grading.service.GradingCommands.AiRequest;
import edu.aiplatform.grading.service.GradingCommands.ItemVersion;
import edu.aiplatform.grading.service.GradingProposalService;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP binding/validation only; service authorization remains covered by the grading service tests. */
class GradingProposalApiContractTest {
    private final GradingProposalService gradingProposalService = mock(GradingProposalService.class);
    private final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final UUID evaluationId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        mvc = MockMvcBuilders.standaloneSetup(new GradingProposalController(gradingProposalService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        when(gradingProposalService.requestProposal(eq(actor), eq(evaluationId), any()))
                .thenReturn(new GradingProposal());
        when(gradingProposalService.requestProposals(eq(actor), any())).thenReturn(new BatchResult(List.of()));
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptsAutomaticSourceForSingleAndBatchRequests(boolean batch) throws Exception {
        mvc.perform(post(requestPath(batch)).contentType("application/json")
                .content(requestBody(batch, "{\"requestRef\":\"auto\"}"))).andExpect(status().isOk());
        verifyRequest(batch, new AiRequest(null, null, "auto"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesExplicitPersonalSourceRequests(boolean batch) throws Exception {
        mvc.perform(post(requestPath(batch)).contentType("application/json")
                .content(requestBody(batch, "{\"source\":\"PERSONAL\",\"provider\":\"OPENAI\",\"requestRef\":\"explicit\"}")))
                .andExpect(status().isOk());
        verifyRequest(batch, new AiRequest(AiSource.PERSONAL, AiProvider.OPENAI, "explicit"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stillRejectsBlankRequestReferences(boolean batch) throws Exception {
        mvc.perform(post(requestPath(batch)).contentType("application/json")
                .content(requestBody(batch, "{\"requestRef\":\" \"}"))).andExpect(status().isBadRequest());
        verifyNoInteractions(gradingProposalService);
    }

    @Test
    void stillRequiresTheNestedBatchRequest() throws Exception {
        mvc.perform(post(requestPath(true)).contentType("application/json")
                .content("{\"items\":[{\"evaluationId\":\"" + evaluationId + "\",\"version\":0}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(gradingProposalService);
    }

    private void verifyRequest(boolean batch, AiRequest request) {
        if (batch) {
            verify(gradingProposalService).requestProposals(actor,
                    new AiBatch(List.of(new ItemVersion(evaluationId, 0)), request));
        } else {
            verify(gradingProposalService).requestProposal(actor, evaluationId, request);
        }
        verifyNoMoreInteractions(gradingProposalService);
    }

    private String requestPath(boolean batch) {
        return batch ? "/api/v1/evaluations:ai-proposals" : "/api/v1/evaluations/" + evaluationId + "/ai-proposals";
    }

    private String requestBody(boolean batch, String request) {
        return batch ? "{\"items\":[{\"evaluationId\":\"" + evaluationId + "\",\"version\":0}],\"request\":" + request + "}" : request;
    }
}
