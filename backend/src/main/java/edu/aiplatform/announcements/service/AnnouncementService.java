package edu.aiplatform.announcements.service;

import edu.aiplatform.announcements.dto.AnnouncementViews;
import edu.aiplatform.announcements.model.*;
import edu.aiplatform.announcements.repository.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(timeout=10)
public class AnnouncementService {
    private final IAnnouncementRepository repository;
    private final AnnouncementAccessPolicy access;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IAudit audit;
    private final IEventPublisher events;
    private final Clock clock;
    public AnnouncementService(IAnnouncementRepository repository,AnnouncementAccessPolicy access,IClassAccess classes,IAccountLookup accounts,IAudit audit,IEventPublisher events,Clock clock) {
        this.repository=repository;this.access=access;this.classes=classes;this.accounts=accounts;this.audit=audit;this.events=events;this.clock=clock;
    }
    @Transactional(readOnly=true,timeout=10)
    public AnnouncementViews.Feed list(Actor actor,UUID classId,int page,int size) {
        if(page<1||page>100000||size<1||size>50)throw ApiException.badRequest("PAGE_INVALID","Trang phải từ1, tối đa50 mục");
        var all=access.list(actor,null);if(classId!=null)access.require(actor,classId,false);
        var allowed=all.stream().filter(c->classId==null||c.classId().equals(classId)).toList();var byId=new HashMap<UUID,ClassRef>();allowed.forEach(c->byId.put(c.classId(),c));
        var rows=repository.list(allowed.stream().map(ClassRef::classId).toList(),page,size);
        boolean more=rows.size()==size&&!repository.list(allowed.stream().map(ClassRef::classId).toList(),page+1,size).isEmpty();
        var items=rows.stream().map(a->view(actor,a,byId.get(a.classId))).toList();
        var options=all.stream().map(c->new AnnouncementViews.ClassOption(c.classId(),c.code(),c.name(),actor.role()!=Role.STUDENT&&Objects.equals(c.teacherId(),actor.accountId()),actor.role()==Role.STUDENT?0:classes.listActiveStudents(c.classId()).size())).toList();
        return new AnnouncementViews.Feed(items,options,page,size,more);
    }
    public AnnouncementViews.Item create(Actor actor,UUID classId,AnnouncementCommands.Create input) {
        var c=access.require(actor,classId,true);var a=Announcement.create(classId,actor.accountId(),input.title(),input.body(),input.important(),input.sendEmail(),clock.instant());
        repository.insert(a);record(actor,"ANNOUNCEMENT_CREATED",a,metadata(a));
        events.publish(new DomainEvent(1,UUID.randomUUID(),EventTypes.CLASS_ANNOUNCEMENT_POSTED,"U14",clock.instant(),a.id.toString(),
                Map.of("announcementId",a.id.toString(),"classId",classId.toString(),"actorId",actor.accountId().toString(),"important",a.important,"sendEmail",a.sendEmail)));
        return view(actor,a,c);
    }
    public AnnouncementViews.Item edit(Actor actor,UUID id,AnnouncementCommands.Edit input) {
        var a=load(actor,id);var c=access.require(actor,a.classId,true);checkVersion(a,input.version());var before=metadata(a);
        a.edit(input.title(),input.body(),actor.accountId(),clock.instant());save(a,input.version());
        record(actor,"ANNOUNCEMENT_UPDATED",a,Map.of("before",before,"after",metadata(a)));return view(actor,a,c);
    }
    public void delete(Actor actor,UUID id,long version) {
        var a=load(actor,id);access.require(actor,a.classId,true);checkVersion(a,version);a.delete(actor.accountId(),clock.instant());save(a,version);
        record(actor,"ANNOUNCEMENT_DELETED",a,metadata(a));
    }
    private Announcement load(Actor actor,UUID id) {return repository.find(id).filter(a->a.status==Announcement.Status.ACTIVE).orElseThrow(()->access.denied(actor,id));}
    private void checkVersion(Announcement a,long version) {if(version<0||a.version!=version)throw ApiException.conflict("ANNOUNCEMENT_STALE","Thông báo đã thay đổi, hãy tải lại");}
    private void save(Announcement a,long version) {if(!repository.update(a,version))throw ApiException.conflict("ANNOUNCEMENT_STALE","Thông báo đã thay đổi, hãy tải lại");}
    private AnnouncementViews.Item view(Actor actor,Announcement a,ClassRef c) {
        String author=accounts.getContact(a.createdBy).map(AccountContact::displayName).orElse("Giảng viên");
        return new AnnouncementViews.Item(a.id,a.classId,c.name(),a.title,a.body,a.important,a.sendEmail,a.createdBy,author,a.createdAt,a.updatedAt,a.version,actor.role()!=Role.STUDENT&&Objects.equals(c.teacherId(),actor.accountId()));
    }
    private Map<String,Object> metadata(Announcement a) {return Map.of("classId",a.classId.toString(),"titleLength",a.title.length(),"bodyLength",a.body.length(),"contentHash",hash(a.title+"\n"+a.body),"important",a.important,"sendEmail",a.sendEmail,"version",a.version,"status",a.status.name());}
    private String hash(String text) {try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private void record(Actor actor,String action,Announcement a,Map<String,Object> details) {audit.record(new AuditEvent(UUID.randomUUID(),actor.accountId(),action,"ANNOUNCEMENT",a.id.toString(),AuditResult.SUCCESS,null,details,clock.instant()));}
}
