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
            if("stale".equals(in.get("requestRef")))throw ApiException.conflict("STALE_PROPOSAL","changed");
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
        when(gradingMcpQueryService.loadGradingContext(eq(actor),anyMap(),eq(false))).thenReturn(Map.of("text","x".repeat(120000),"complete",true));
        var result=(Map<String,Object>)gradingMcpBatchService.call(actor,"read_grading_batch",Map.of("items",List.of(
                Map.of("evaluationId",UUID.randomUUID().toString()),Map.of("evaluationId",UUID.randomUUID().toString())))).content();
        var items=(List<Map<String,Object>>)result.get("items");
        assertThat(items.get(0)).containsEntry("outcome","OK");
        assertThat(items.get(1)).containsEntry("code","MCP_BATCH_RESULT_LIMIT").doesNotContainKey("result");
        verify(gradingMcpQueryService,times(1)).recordRead(eq(actor),anyMap());
    }
    @Test void escapedTransportEnvelopeCannotMarkAnUndeliveredPageAsRead() {
        when(gradingMcpQueryService.loadGradingContext(eq(actor),anyMap(),eq(false)))
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
}
