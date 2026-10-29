package edu.aiplatform.academics.application;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.academics.domain.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.subjects.contract.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AcademicsMcpTools implements IMcpToolProvider {
    private final IAcademicsReadRepository reads;
    private final ISubjectQuery subjects;
    public AcademicsMcpTools(IAcademicsReadRepository reads,ISubjectQuery subjects){this.reads=reads;this.subjects=subjects;}
    @Override public List<McpToolDefinition> tools(){
        return List.of(McpSchemas.read("list_my_classes","Tìm lớp được phân công giảng dạy, không gồm lớp ngoài quyền.",
                McpSchemas.PAGE+",\"query\":"+McpSchemas.TEXT+",\"status\":{\"enum\":[\"DRAFT\",\"OPEN\",\"ARCHIVED\"]}"),
                McpSchemas.read("list_my_subjects","Tìm môn từ lớp được dạy và môn được giao phụ trách.",McpSchemas.PAGE+",\"query\":"+McpSchemas.TEXT));
    }
    @Override @Transactional(readOnly=true)
    public McpToolResult call(Actor actor,String name,Map<String,Object> args){
        McpInput.teaching(actor);var in=new McpInput(args);int page=in.page(),size=in.size();
        String query=in.text("query","").trim().toLowerCase(Locale.ROOT);
        if(query.length()>200)throw McpInput.invalid();
        var taught=reads.listTaught(actor.accountId()).stream().filter(c->actor.accountId().equals(c.teacherId())).toList();
        if("list_my_classes".equals(name)){
            var status=in.choice("status",edu.aiplatform.academics.contract.ClassStatus.class,null);
            var rows=taught.stream().filter(c->status==null||c.status()==status)
                    .filter(c->(c.code()+" "+c.name()).toLowerCase(Locale.ROOT).contains(query))
                    .map(c->Map.of("classId",c.id(),"subjectId",c.subjectId(),"code",c.code(),"title",c.name(),"status",c.status(),"version",c.version())).toList();
            return new McpToolResult(false,McpInput.page(rows,page,size));
        }
        if(!"list_my_subjects".equals(name))throw McpInput.invalid();
        Map<UUID,SubjectRef> found=new LinkedHashMap<>();
        for(var c:taught)subjects.getSubject(c.subjectId()).ifPresent(s->found.put(s.subjectId(),s));
        if(actor.role()==Role.SUBJECT_MANAGER)subjects.listManagedSubjects(actor.accountId()).stream()
                .filter(s->actor.accountId().equals(s.managerId())).forEach(s->found.put(s.subjectId(),s));
        var rows=found.values().stream().filter(s->(s.code()+" "+s.name()).toLowerCase(Locale.ROOT).contains(query))
                .map(s->Map.of("subjectId",s.subjectId(),"code",s.code(),"title",s.name(),"status",s.status(),"canManageSubject",actor.accountId().equals(s.managerId()))).toList();
        return new McpToolResult(false,McpInput.page(rows,page,size));
    }
}
