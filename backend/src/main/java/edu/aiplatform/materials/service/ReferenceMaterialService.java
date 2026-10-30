package edu.aiplatform.materials.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true,timeout=10,noRollbackFor=ApiException.class)
public class ReferenceMaterialService implements IReferenceMaterial {
    public static final int MAX_CHARS=1_000_000;
    private final MaterialQueryService queries;
    private final IMaterialRepository repository;
    public ReferenceMaterialService(MaterialQueryService queries,IMaterialRepository repository) {
        this.queries=queries;this.repository=repository;
    }
    @Override public List<SelectableMaterial> listSelectable(Actor actor,ResourceRef scope) {
        if(scope==null||scope.id()==null)throw ApiException.badRequest("MATERIAL_SCOPE_REQUIRED","Chọn môn hoặc lớp");
        List<MaterialViews.Detail> items=switch(scope.scope()) {
            case SUBJECT -> queries.subjectList(actor,scope.id());
            case CLASS -> queries.classList(actor,scope.id(),actor!=null&&actor.role()==Role.STUDENT,null);
            default -> throw ApiException.badRequest("MATERIAL_SCOPE_INVALID","Chọn môn hoặc lớp");
        };
        return items.stream().filter(m->m.status()==Material.Status.ACTIVE)
                .map(m->new SelectableMaterial(m.id(),m.title(),m.current().textStatus()==MaterialVersion.TextStatus.EXTRACTED)).toList();
    }
    @Override public List<MaterialText> extractText(Actor actor,List<UUID> ids,int maxChars) {
        if(ids==null||ids.size()>10||ids.stream().anyMatch(Objects::isNull)||maxChars<1)
            throw ApiException.badRequest("MATERIAL_TEXT_LIMIT","Chọn tối đa 10 học liệu và giới hạn ký tự hợp lệ");
        // Authorize every ID before reading or exposing any text.
        List<Material> materials=ids.stream().distinct().map(id->queries.authorized(actor,id,null)).toList();
        if(materials.stream().anyMatch(m->m.status()!=Material.Status.ACTIVE))throw ApiException.notFound("MATERIAL_NOT_FOUND");
        int remaining=Math.min(maxChars,MAX_CHARS);List<MaterialText> result=new ArrayList<>();
        for(Material m:materials) {
            MaterialVersion v=repository.version(m.id(),m.currentVersionNo()).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
            String text=m.kind()==Material.Kind.YOUTUBE_LINK ? m.title()+"\n"+m.description()+"\n"+v.videoUrl()
                    : v.textStatus()==MaterialVersion.TextStatus.EXTRACTED?Objects.toString(v.extractedText(),""):"";
            int length=Math.min(remaining,text.length());
            result.add(new MaterialText(m.id(),m.title(),text.substring(0,length),length<text.length()));remaining-=length;
        }
        return result;
    }
}
