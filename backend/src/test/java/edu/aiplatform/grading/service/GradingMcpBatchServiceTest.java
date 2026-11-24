package edu.aiplatform.grading.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;
class GradingMcpBatchServiceTest {
    final GradingMcpQueryService gradingMcpQueryService=mock(GradingMcpQueryService.class);
    final GradingMcpTools gradingMcpTools=mock(GradingMcpTools.class);
    final Actor actor=new Actor(UUID.randomUUID(),Role.TEACHER);
    final GradingMcpBatchService gradingMcpBatchService=new GradingMcpBatchService(gradingMcpQueryService,gradingMcpTools,new ObjectMapper());
    @Test void failedAndRepeatedItemsDoNotDiscardSuccessfulWrites() {
        when(gradingMcpTools.call(eq(actor),anyString(),anyMap())).thenAnswer(call -> {
            Map<String,Object> in=call.getArgument(2);
            if("stale".equals(in.get("requestRef")))throw ApiException.createConflict("STALE_PROPOSAL","changed");
            return new McpToolResult(false,Map.of("proposalId","proposal-"+in.get("requestRef")));
        });
        var result=(Map<String,Object>)gradingMcpBatchService.call(actor,"record_grading_proposals_batch",Map.of("items",List.of(
                Map.of("requestRef","valid"),Map.of("requestRef","stale"),Map.of("requestRef","valid"),Map.of("requestRef","another")))).content();
        var items=(List<Map<String,Object>>)result.get("items");
        assertThat(items).extracting(i -> i.get("outcome")).containsExactly("OK","ERROR","ERROR","OK");
        assertThat(items.get(1)).containsEntry("code","STALE_PROPOSAL");
        assertThat(items.get(2)).containsEntry("code","DUPLICATE_ITEM");
        assertThat(result.get("partial")).isEqualTo(true);
    }
    @Test void aggregateReadBudgetReturnsRetryableErrorInsteadOfTruncatedContext() {
        when(gradingMcpQueryService.loadGradingContext(eq(actor),anyMap(),eq(false),anyInt())).thenReturn(Map.of("text","x".repeat(120000),"complete",true));
        var result=(Map<String,Object>)gradingMcpBatchService.call(actor,"read_grading_batch",Map.of("items",List.of(
                Map.of("evaluationId",UUID.randomUUID().toString()),Map.of("evaluationId",UUID.randomUUID().toString())))).content();
        var items=(List<Map<String,Object>>)result.get("items");
        assertThat(items.get(0)).containsEntry("outcome","OK");
        assertThat(items.get(1)).containsEntry("code","MCP_BATCH_RESULT_LIMIT").doesNotContainKey("result");
        verify(gradingMcpQueryService,times(1)).recordRead(eq(actor),anyMap());
    }
    @Test void escapedTransportEnvelopeCannotMarkAnUndeliveredPageAsRead() {
        when(gradingMcpQueryService.loadGradingContext(eq(actor),anyMap(),eq(false),anyInt()))
                .thenReturn(Map.of("text","\\".repeat(50000),"complete",true))
                .thenReturn(Map.of("text","x".repeat(68000),"complete",true));
        var result=(Map<String,Object>)gradingMcpBatchService.call(actor,"read_grading_batch",Map.of("items",List.of(
                Map.of("evaluationId",UUID.randomUUID().toString()),Map.of("evaluationId",UUID.randomUUID().toString())))).content();
        var items=(List<Map<String,Object>>)result.get("items");
        assertThat(items.get(1).get("code")).isEqualTo("MCP_BATCH_RESULT_LIMIT");
        assertThat(items.get(1)).doesNotContainKey("result");
        try {
            String text=new ObjectMapper().writeValueAsString(result);
            assertThat(new ObjectMapper().writeValueAsBytes(Map.of("content",List.of(Map.of("type","text","text",text)),"isError",false)).length)
                    .isLessThan(256*1024-512);
        } catch(Exception ex){throw new AssertionError(ex);}
        verify(gradingMcpQueryService,times(1)).recordRead(eq(actor),anyMap());
    }
    @Test void laterReadItemsGetOnlyTheBatchSpaceLeft() {
        when(gradingMcpQueryService.loadGradingContext(eq(actor),anyMap(),eq(false),anyInt()))
                .thenReturn(Map.of("text","x".repeat(100000),"complete",true))
                .thenReturn(Map.of("text","small","complete",false));
        var result=(Map<String,Object>)gradingMcpBatchService.call(actor,"read_grading_batch",Map.of("items",List.of(
                Map.of("evaluationId",UUID.randomUUID().toString()),Map.of("evaluationId",UUID.randomUUID().toString())))).content();
        var budgets=org.mockito.ArgumentCaptor.forClass(Integer.class);
        verify(gradingMcpQueryService,times(2)).loadGradingContext(eq(actor),anyMap(),eq(false),budgets.capture());
        assertThat(budgets.getAllValues().get(0)).isEqualTo(GradingMcpQueryService.CONTEXT_BYTES);
        assertThat(budgets.getAllValues().get(1)).isBetween(1,GradingMcpBatchService.BATCH_CONTENT_BYTES-100000);
        assertThat((List<Map<String,Object>>)result.get("items")).extracting(i -> i.get("outcome")).containsExactly("OK","OK");
    }
    @Test void blockThatOnlyFitsAnEmptyBatchIsRetryableBatchLimit() {
        when(gradingMcpQueryService.loadGradingContext(eq(actor),anyMap(),eq(false),anyInt()))
                .thenReturn(Map.of("text","x".repeat(100000),"complete",true))
                .thenThrow(ApiException.createBadRequest("MCP_CONTEXT_TOO_LARGE","too large"));
        var result=(Map<String,Object>)gradingMcpBatchService.call(actor,"read_grading_batch",Map.of("items",List.of(
                Map.of("evaluationId",UUID.randomUUID().toString()),Map.of("evaluationId",UUID.randomUUID().toString())))).content();
        var items=(List<Map<String,Object>>)result.get("items");
        assertThat(items.get(1)).containsEntry("code","MCP_BATCH_RESULT_LIMIT");
    }
    @Test void escapeHeavyEarlierItemsShrinkTheNextBudgetBelowTheRawSpaceLeft() {
        when(gradingMcpQueryService.loadGradingContext(eq(actor),anyMap(),eq(false),anyInt()))
                .thenReturn(Map.of("text","\"".repeat(40000),"complete",true))
                .thenReturn(Map.of("text","small","complete",false));
        gradingMcpBatchService.call(actor,"read_grading_batch",Map.of("items",List.of(
                Map.of("evaluationId",UUID.randomUUID().toString()),Map.of("evaluationId",UUID.randomUUID().toString()))));
        var budgets=org.mockito.ArgumentCaptor.forClass(Integer.class);
        verify(gradingMcpQueryService,times(2)).loadGradingContext(eq(actor),anyMap(),eq(false),budgets.capture());
        // 40k quotes are ~80 KB of JSON but ~160 KB once escaped into the envelope text.
        int rawLeft=GradingMcpBatchService.BATCH_CONTENT_BYTES-80000;
        assertThat(budgets.getAllValues().get(1)).isBetween(1,rawLeft/2);
    }
}
