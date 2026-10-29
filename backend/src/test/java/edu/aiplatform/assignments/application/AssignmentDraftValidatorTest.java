package edu.aiplatform.assignments.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.GradingMode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AssignmentDraftValidatorTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void acceptsBilingualProfileWhenQuestionAndRubricTotalsMatch() throws Exception {
        JsonNode proposal = json.readTree("""
                {"schemaVersion":2,"title":"Online Pharmacy Management","businessContext":"Customers search and order medicines; pharmacists manage stock and verify prescriptions.","instructions":"Answer in English.","sections":[
                  {"level":1,"title":"Context diagram","guidance":"Show external actors and data flows.","rubric":[{"title":"Coverage","kind":"DIAGRAM","items":[{"text":"Actors and boundaries are clear","points":4}]}]},
                  {"level":1,"title":"Use case diagram","guidance":"Model customer and pharmacist use cases.","rubric":[{"title":"Use cases","kind":"DIAGRAM","items":[{"text":"Main and exception flows are covered","points":6}]}]}]}
                """);

        JsonNode result = AssignmentDraftValidator.validate(proposal, GradingMode.GRADED,
                new AssignmentDraftValidator.Options("SWR", "EN", 2, new BigDecimal("10.00"), true));

        assertThat(result.path("status").asText()).isEqualTo("VALID");
        assertThat(result.path("questionCount").asInt()).isEqualTo(2);
        assertThat(result.path("rubricLeafCount").asInt()).isEqualTo(2);
        assertThat(result.path("totalPoints").decimalValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void rejectsSWRProposalWithoutBusinessContext() throws Exception {
        JsonNode proposal = json.readTree("""
                {"schemaVersion":2,"title":"Pharmacy","businessContext":"","instructions":"","sections":[
                  {"level":1,"title":"Question 1","guidance":"Do the work.","rubric":[{"title":"Quality","kind":"TEXT","items":[{"text":"Clear","points":10}]}]}]}
                """);

        JsonNode result = AssignmentDraftValidator.validate(proposal, GradingMode.GRADED,
                new AssignmentDraftValidator.Options("SWR", "EN", 1, new BigDecimal("10"), true));

        assertThat(result.path("status").asText()).isEqualTo("INVALID");
        assertThat(issueCodes(result)).contains("BUSINESS_CONTEXT_REQUIRED");
    }

    @Test
    void rejectsParentRubricThatWouldNotBeAppliedToAQuestionLeaf() throws Exception {
        JsonNode proposal = json.readTree("""
                {"schemaVersion":2,"title":"Event Management","businessContext":"FPT runs events with limited capacity.","instructions":"","sections":[
                  {"level":1,"title":"Event design","guidance":"","rubric":[{"title":"Parent rubric","kind":"TEXT","items":[{"text":"Ignored by leaf scoring","points":10}]}]},
                  {"level":2,"title":"Registration flow","guidance":"","rubric":[]}]}
                """);

        JsonNode result = AssignmentDraftValidator.validate(proposal, GradingMode.GRADED,
                new AssignmentDraftValidator.Options("SWD", "EN", 1, new BigDecimal("10"), true));

        assertThat(result.path("status").asText()).isEqualTo("INVALID");
        assertThat(issueCodes(result)).contains("RUBRIC_ON_NON_LEAF", "RUBRIC_REQUIRED");
    }

    @Test
    void rejectsQuestionCountAndDecimalTotalMismatch() throws Exception {
        JsonNode proposal = json.readTree("""
                {"schemaVersion":2,"title":"Clinic booking","businessContext":"Patients book appointments.","instructions":"","sections":[
                  {"level":1,"title":"Booking","guidance":"","rubric":[{"title":"Flow","kind":"TEXT","items":[{"text":"Success path","points":4.25},{"text":"Failure path","points":6.75}]}]}]}
                """);

        JsonNode result = AssignmentDraftValidator.validate(proposal, GradingMode.GRADED,
                new AssignmentDraftValidator.Options("SWD", "EN", 2, new BigDecimal("10"), true));

        assertThat(result.path("status").asText()).isEqualTo("INVALID");
        assertThat(issueCodes(result)).contains("QUESTION_COUNT_MISMATCH", "TOTAL_POINTS_MISMATCH");
        assertThat(result.path("totalPoints").decimalValue()).isEqualByComparingTo("11.00");
    }

    @Test
    void rejectsRunawayProseBeforeItCanBeApplied() throws Exception {
        var proposal = json.createObjectNode();
        proposal.put("businessContext", "x".repeat(4001));
        proposal.put("instructions", "x".repeat(1201));
        proposal.putArray("sections");
        JsonNode result = AssignmentDraftValidator.validate(proposal, GradingMode.GRADED,
                new AssignmentDraftValidator.Options("AUTO", "EN", 3, new BigDecimal("10"), true));
        assertThat(issueCodes(result)).contains("CONTEXT_TOO_LONG", "INSTRUCTIONS_TOO_LONG", "PROPOSAL_STRUCTURE_INVALID");
    }

    private static java.util.List<String> issueCodes(JsonNode result) {
        java.util.List<String> codes = new java.util.ArrayList<>();
        result.path("issues").forEach(issue -> codes.add(issue.path("code").asText()));
        return codes;
    }
}
