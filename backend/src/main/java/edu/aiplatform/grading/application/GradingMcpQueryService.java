package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.*;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.grading.domain.EvaluationKind;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Authorized immutable grading snapshots; pages never silently discard structured document blocks. */
@Component
public class GradingMcpQueryService implements IMcpToolProvider {
    static final int CONTEXT_BYTES=128*1024;
    static final String CONTEXT_INPUT="\"evaluationId\":"+McpSchemas.UUID
            +",\"offset\":{\"type\":\"integer\",\"minimum\":0},\"expectedVersion\":{\"type\":\"integer\",\"minimum\":0}"
            +",\"submissionHash\":{\"type\":\"string\",\"maxLength\":200},\"snapshotFingerprint\":{\"type\":\"string\",\"maxLength\":64}";
    private final SubmissionReviewService reviews;
    private final GradingPromptBuilder prompts;
    private final GradingProposalService proposals;
    private final GradebookService gradebook;
    private final ObjectMapper json;
    public GradingMcpQueryService(SubmissionReviewService reviews,GradingPromptBuilder prompts,
            GradingProposalService proposals,GradebookService gradebook,ObjectMapper json) {
        this.reviews=reviews;this.prompts=prompts;this.proposals=proposals;this.gradebook=gradebook;this.json=json;
    }
    @Override public List<McpToolDefinition> tools() {
        String scope="\"classId\":"+McpSchemas.UUID+",\"assignmentId\":"+McpSchemas.UUID;
        return List.of(
                McpSchemas.read("read_grading_context","Đọc snapshot để chấm. Chỉ chấm khi complete=true và đã ghép đủ các trang cùng fingerprint.",CONTEXT_INPUT,"evaluationId"),
                McpSchemas.read("list_submissions","Tìm bài nộp được phép; evaluationId null nghĩa là chưa có snapshot để chấm.",scope+","+McpSchemas.PAGE
                        +",\"state\":{\"enum\":[\"ALL\",\"PENDING\",\"DRAFT\",\"FINALIZED\",\"PUBLISHED\",\"NOT_SUBMITTED\",\"IN_PROGRESS\"]}"
                        +",\"late\":{\"type\":\"boolean\"},\"type\":{\"enum\":[\"ATTEMPT\",\"GROUP_DOCUMENT\"]},\"proposalStatus\":"+McpSchemas.TEXT,"classId","assignmentId"),
                McpSchemas.read("list_grading_proposals","Đọc trạng thái đề xuất của evaluation được phép; không trả nội dung bài nộp.","\"evaluationId\":"+McpSchemas.UUID+","+McpSchemas.PAGE,"evaluationId"),
                McpSchemas.read("read_class_learning_summary","Thống kê nộp/chấm theo snapshot hiện hành, có nguồn và thời điểm tính; không tự suy diễn chất lượng học tập.",scope,"classId"),
                McpSchemas.read("read_grade_distribution","Phân bố điểm đã công bố từ GradebookService.",scope,"classId","assignmentId"));
    }
    @Override @Transactional(isolation=Isolation.REPEATABLE_READ,timeout=20)
    public McpToolResult call(Actor actor,String name,Map<String,Object> args) {
        McpInput.teaching(actor);var in=new McpInput(args);
        return new McpToolResult(false,switch(name) {
            case "read_grading_context" -> context(actor,args);
            case "list_submissions" -> submissions(actor,in);
            case "list_grading_proposals" -> {
                UUID id=in.uuid("evaluationId");reviews.detail(actor,id);
                var rows=proposals.list(actor,id).stream().map(p -> {
                    Map<String,Object> row=new LinkedHashMap<>();row.put("proposalId",p.id);row.put("status",p.status);
                    row.put("origin",p.origin);row.put("createdAt",p.createdAt);row.put("completedAt",p.completedAt);
                    row.put("usedAt",p.usedAt);return row;
                }).toList();yield McpInput.page(rows,in.page(),in.size());
            }
            case "read_grade_distribution" -> Map.of("classId",in.uuid("classId"),"assignmentId",in.uuid("assignmentId"),
                    "source","PUBLISHED_GRADES","calculatedAt",Instant.now(),"status","AVAILABLE",
                    "buckets",gradebook.distribution(actor,in.uuid("classId"),in.uuid("assignmentId"),false));
            case "read_class_learning_summary" -> {
                var classId=in.uuid("classId");var out=new LinkedHashMap<String,Object>();out.put("classId",classId);
                out.put("calculatedAt",Instant.now());out.put("status","AVAILABLE");out.put("source","CURRENT_SUBMISSION_AND_GRADING_STATE");
                if(in.has("assignmentId")) {
                    out.putAll(assignmentSummary(actor,classId,in.uuid("assignmentId"),100));
                } else {
                    var page=gradebook.list(actor,classId,null,1,1);
                    out.put("studentCount",page.total());out.put("assignmentCount",page.assignments().size());
                    var summaries=page.assignments().stream().limit(10).map(a -> assignmentSummary(actor,classId,a.id(),20)).toList();
                    out.put("assignments",summaries);
                    if(page.assignments().size()>10||summaries.stream().anyMatch(s -> "PARTIAL".equals(s.get("status"))))out.put("status","PARTIAL");
                    out.put("detailHint","Use assignmentId for up to 100 submissions per assignment; omitted coverage is explicit.");
                }yield out;
            }
            default -> throw McpInput.invalid();
        });
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public Map<String,Object> context(Actor actor,Map<String,Object> args) {
        McpInput.teaching(actor);var in=new McpInput(args);UUID id=in.uuid("evaluationId");
        int offset=in.integer("offset",0,0,Integer.MAX_VALUE);
        if(offset>0&&(!in.has("expectedVersion")||!in.has("submissionHash")||!in.has("snapshotFingerprint")))throw McpInput.invalid();
        var detail=reviews.detail(actor,id);var e=detail.evaluation();
        if(e.kind==EvaluationKind.MEMBER)throw ApiException.badRequest("GROUP_ROOT_REQUIRED","Read the group document root");
        if(!detail.current())throw ApiException.conflict("SUBMISSION_CHANGED","Snapshot is no longer current");
        if(in.has("expectedVersion"))e.checkVersion(in.version("expectedVersion"));
        if(in.has("submissionHash")&&!Objects.equals(e.submissionHash,in.text("submissionHash",null)))
            throw ApiException.conflict("SUBMISSION_CHANGED","Snapshot changed");
        String fingerprint=McpFingerprint.of(json,Map.of("hash",e.submissionHash,"version",e.version,
                "rubric",e.rubricSnapshot,"document",detail.document()));
        if(in.has("snapshotFingerprint")&&!fingerprint.equals(in.text("snapshotFingerprint",null)))
            throw ApiException.conflict("SUBMISSION_CHANGED","Snapshot or rubric changed; restart reading");
        var blocks=detail.document().blocks();if(offset>blocks.size())throw McpInput.invalid();
        var reference=Map.of("kind",e.kind.name(),"id",(e.kind==EvaluationKind.ATTEMPT?e.attemptId:e.groupDocumentId).toString(),
                "classId",e.classId.toString(),"assignmentId",e.assignmentId.toString());
        Map<String,Object> out=new LinkedHashMap<>();out.put("evaluationId",id.toString());out.put("expectedVersion",e.version);
        out.put("submissionRef",reference);out.put("submissionHash",e.submissionHash);out.put("snapshotFingerprint",fingerprint);
        try {out.put("rubricSnapshot",json.readTree(e.rubricSnapshot));out.put("outputSchema",json.readTree(prompts.outputSchema(e)));}
        catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException("Invalid grading schema",ex);}
        out.put("untrustedContent",true);out.put("offset",offset);out.put("totalBlocks",blocks.size());
        List<Object> selected=new ArrayList<>();out.put("documentBlocks",selected);
        int next=offset;out.put("complete",false);
        // Reserve space for continuation before accumulating blocks.
        out.put("continuation",continuation(id,e.version,e.submissionHash,fingerprint,next));
        if(bytes(out)>CONTEXT_BYTES)throw tooLarge();
        while(next<blocks.size()) {
            selected.add(blocks.get(next));
            if(bytes(out)>CONTEXT_BYTES) {selected.remove(selected.size()-1);break;}
            next++;
        }
        if(next==offset&&next<blocks.size())throw tooLarge();
        boolean complete=next==blocks.size();out.put("complete",complete);
        out.put("continuation",complete?null:continuation(id,e.version,e.submissionHash,fingerprint,next));
        return out;
    }
    private Map<String,Object> submissions(Actor actor,McpInput in) {
        UUID classId=in.uuid("classId"),assignmentId=in.uuid("assignmentId");int page=in.page(),size=in.size();
        String state=in.text("state","ALL");Boolean late=in.get("late")==null?null:bool(in.get("late"));
        // Filtering precedes pagination, so totals never describe a different scope or filter.
        var first=reviews.list(actor,classId,assignmentId,state,late,1,100);
        List<GradingViews.SubmissionRow> all=new ArrayList<>(first.rows());
        if(first.total()>10000)throw ApiException.badRequest("MCP_DISCOVERY_TOO_LARGE","Narrow the status filter");
        for(int p=2;all.size()<first.total();p++) {
            var more=reviews.list(actor,classId,assignmentId,state,late,p,100);if(more.rows().isEmpty())break;all.addAll(more.rows());
        }
        List<Map<String,Object>> rows=new ArrayList<>();
        String type=in.text("type",null),proposalState=in.text("proposalStatus",null);
        if(type!=null&&!Set.of("ATTEMPT","GROUP_DOCUMENT").contains(type))throw McpInput.invalid();
        for(var row:all) {
            Map<String,Object> mapped=new LinkedHashMap<>();mapped.put("student",row.student());mapped.put("evaluationId",row.evaluationId());
            mapped.put("status",row.status());mapped.put("expectedVersion",row.version());mapped.put("late",row.late());mapped.put("submittedAt",row.submittedAt());
            if(row.evaluationId()!=null) {
                var e=reviews.detail(actor,row.evaluationId()).evaluation();
                if(e.kind==EvaluationKind.MEMBER||(type!=null&&!type.equals(e.kind.name())))continue;
                if(proposalState!=null&&proposals.list(actor,e.id).stream().noneMatch(p -> proposalState.equals(p.status)))continue;
                mapped.put("submissionRef",Map.of("kind",e.kind.name(),"id",(e.kind==EvaluationKind.ATTEMPT?e.attemptId:e.groupDocumentId),"classId",e.classId,"assignmentId",e.assignmentId));
                mapped.put("submissionHash",e.submissionHash);
            } else if(type!=null||proposalState!=null)continue;
            rows.add(mapped);
        }
        var out=new LinkedHashMap<>(McpInput.page(rows,page,size));out.put("summary",first.summary());return out;
    }
    private Map<String,Object> assignmentSummary(Actor actor,UUID classId,UUID assignmentId,int limit) {
        var page=reviews.list(actor,classId,assignmentId,"ALL",null,1,limit);
        Map<String,Long> strengths=new HashMap<>(),improvements=new HashMap<>();int covered=0;
        for(var row:page.rows()) {
            if(row.evaluationId()==null)continue;
            var evaluation=reviews.detail(actor,row.evaluationId()).evaluation();
            if(evaluation.kind==EvaluationKind.MEMBER||evaluation.structuredFeedback==null||evaluation.structuredFeedback.summary()==null)continue;
            covered++;var summary=evaluation.structuredFeedback.summary();
            tally(strengths,summary.strengths());tally(improvements,summary.improvements());
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("assignmentId",assignmentId);out.put("summary",page.summary());
        out.put("feedbackAggregation",Map.of("strengths",top(strengths),"improvements",top(improvements),
                "method","EXACT_TEXT_FREQUENCY_FROM_EXISTING_STRUCTURED_FEEDBACK"));
        out.put("feedbackCovered",covered);out.put("submissionsInspected",page.rows().size());out.put("totalSubmissions",page.total());
        out.put("feedbackComplete",page.total()<=limit);out.put("status",page.total()>limit?"PARTIAL":"AVAILABLE");return out;
    }
    private static void tally(Map<String,Long> counts,List<String> values) {
        if(values!=null)values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct()
                .forEach(s -> counts.merge(s,1L,Long::sum));
    }
    private static List<Map<String,Object>> top(Map<String,Long> counts) {
        return counts.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(20).map(e -> Map.<String,Object>of("text",e.getKey().substring(0,Math.min(500,e.getKey().length())),
                        "count",e.getValue(),"truncated",e.getKey().length()>500)).toList();
    }
    private static boolean bool(Object value){if(!(value instanceof Boolean b))throw McpInput.invalid();return b;}
    private static Map<String,Object> continuation(UUID id,long version,String hash,String fingerprint,int offset) {
        return Map.of("evaluationId",id.toString(),"expectedVersion",version,"submissionHash",hash,"snapshotFingerprint",fingerprint,"offset",offset);
    }
    private int bytes(Object value) {try{return json.writeValueAsBytes(value).length;}catch(Exception e){throw new IllegalStateException(e);}}
    private static ApiException tooLarge(){return ApiException.badRequest("MCP_CONTEXT_TOO_LARGE","Rubric, schema or one document block is too large; manual review is required");}
}
