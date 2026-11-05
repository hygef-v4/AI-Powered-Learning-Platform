package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** No batch-wide transaction: each proposal uses recordMcp's own transaction and idempotency checks. */
@Component
public class GradingMcpBatchService implements IMcpToolProvider {
    private final GradingMcpQueryService gradingMcpQueryService;
    private final GradingMcpTools gradingMcpTools;
    private final ObjectMapper objectMapper;
    public GradingMcpBatchService(GradingMcpQueryService gradingMcpQueryService,GradingMcpTools gradingMcpTools,ObjectMapper objectMapper){this.gradingMcpQueryService=gradingMcpQueryService;this.gradingMcpTools=gradingMcpTools;this.objectMapper=objectMapper;}
    @Override public List<McpToolDefinition> tools() {
        try {
            String item=gradingMcpTools.tools().get(0).parameterSchema();
            return List.of(new McpToolDefinition("read_grading_batch","Đọc tối đa 20 snapshot, lỗi từng item độc lập; dùng continuation để đọc đủ.",
                    gradeBatch(McpSchemas.schema(GradingMcpQueryService.CONTEXT_INPUT,"evaluationId")),McpToolKind.READ,McpSchemas.STAFF),
                    new McpToolDefinition("record_grading_proposals_batch","Lưu tối đa 20 đề xuất độc lập; không lưu hay công bố điểm chính thức.",
                    gradeBatch(item),McpToolKind.WRITE,McpSchemas.STAFF));
        }catch(RuntimeException e){throw new IllegalStateException("Invalid batch schema",e);}
    }
    private static String gradeBatch(String item){return McpSchemas.schema("\"items\":{\"type\":\"array\",\"minItems\":1,\"maxItems\":20,\"items\":"+item+"}","items");}
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public McpToolResult call(Actor actor,String name,Map<String,Object> args) {
        McpInput.teaching(actor);boolean write="record_grading_proposals_batch".equals(name);
        if(!write&&!"read_grading_batch".equals(name))throw McpInput.invalid();
        if(args==null||!(args.get("items") instanceof List<?> items)||items.isEmpty()||items.size()>20)throw McpInput.invalid();
        List<Map<String,Object>> results=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int n=0;n<items.size();n++) {
            Map<String,Object> outcome=new LinkedHashMap<>();outcome.put("index",n);
            try {
                if(!(items.get(n) instanceof Map<?,?> raw))throw McpInput.invalid();
                var input=objectMapper.convertValue(raw,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
                String key=write?Objects.toString(input.get("requestRef"),""):Objects.toString(input.get("evaluationId"),"")+":"+Objects.toString(input.get("offset"),"0");
                if(!seen.add(key))throw ApiException.badRequest("DUPLICATE_ITEM","Repeated item in batch");
                if(input.containsKey("evaluationId"))outcome.put("evaluationId",input.get("evaluationId"));
                if(input.containsKey("requestRef"))outcome.put("requestRef",input.get("requestRef"));
                var result=write?gradingMcpTools.call(actor,"record_grading_proposal",input).content():gradingMcpQueryService.loadGradingContext(actor,input,false);
                outcome.put("outcome","OK");outcome.put("result",result);
                // Leave space for bounded outcomes for all remaining items.
                if(!isWithinBatchLimit(results,outcome)) {
                    outcome.remove("result");outcome.put("outcome","ERROR");outcome.put("code","MCP_BATCH_RESULT_LIMIT");
                } else if(!write)gradingMcpQueryService.recordRead(actor,(Map<String,Object>)result);
            }catch(ApiException e){outcome.remove("result");outcome.put("outcome","ERROR");outcome.put("code",e.code());}
            catch(RuntimeException e){outcome.remove("result");outcome.put("outcome","ERROR");outcome.put("code","MCP_ITEM_UNAVAILABLE");}
            results.add(outcome);
        }
        return new McpToolResult(false,Map.of("items",results,"partial",results.stream().anyMatch(r -> !"OK".equals(r.get("outcome")))));
    }
    private boolean isWithinBatchLimit(List<Map<String,Object>> results,Map<String,Object> next) {
        var candidate=new ArrayList<>(results);candidate.add(next);
        var content=Map.of("items",candidate,"partial",true);
        try {
            String text=objectMapper.writeValueAsString(content);
            // Account for text escaping in the MCP envelope; reserve room for remaining bounded outcomes.
            var envelope=Map.of("content",List.of(Map.of("type","text","text",text)),"isError",false);
            return measureJsonBytes(content)<=180*1024&&measureJsonBytes(envelope)<=240*1024;
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}
    }
    private int measureJsonBytes(Object value){try{return objectMapper.writeValueAsBytes(value).length;}catch(Exception e){throw new IllegalStateException(e);}}
}
