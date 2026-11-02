package edu.aiplatform.notifications.service;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service @Transactional(timeout=15)
public class NotificationService {
    public record Item(UUID id,String type,String title,String summary,String link,Instant readAt,Instant createdAt) { }
    public record Feed(List<Item> items,int page,int size,boolean hasMore) { }
    private final INotificationRepository repository;private final NotificationAccess access;
    private final EmailDeliveryService emails;private final ISseHub sse;private final Clock clock;
    public NotificationService(INotificationRepository repository,NotificationAccess access,EmailDeliveryService emails,ISseHub sse,Clock clock) {this.repository=repository;this.access=access;this.emails=emails;this.sse=sse;this.clock=clock;}
    public void create(UUID eventId,NotificationType type,Collection<UUID> recipients,String title,String summary,String link,String emailType,Instant expiresAt) {
        if(!link.startsWith("/")||link.startsWith("//")||link.contains("\\")||link.length()>500||title.length()>200||summary.length()>500)throw new IllegalArgumentException("Invalid notification content");
        Set<UUID> changed=new HashSet<>();
        for(UUID recipient:new LinkedHashSet<>(recipients)) {
            var n=new Notification();n.id=UUID.randomUUID();n.recipientId=recipient;n.type=type;n.sourceEventId=eventId;n.title=title;n.summary=summary;n.link=link;n.createdAt=clock.instant();
            if(repository.insert(n)){emails.schedule(n,emailType,expiresAt);changed.add(recipient);}
        }
        if(!changed.isEmpty())afterCommit(()->changed.forEach(id->{try{sse.publish(channel(id),Map.of("type","notification-changed"));}catch(RuntimeException ignored){/* REST fallback; committed state stays authoritative. */}}));
    }
    @Transactional(readOnly=true,timeout=5)
    public Feed list(Actor actor,boolean unread,int page,int size) {
        UUID owner=access.self(actor);if(page<1||page>100000||size<1||size>50)throw ApiException.badRequest("PAGE_INVALID","Trang phải từ1, tối đa50 mục");
        var rows=repository.list(owner,unread,page,size);boolean more=rows.size()==size&&!repository.list(owner,unread,page+1,size).isEmpty();
        return new Feed(rows.stream().map(n->new Item(n.id,n.type.name(),n.title,NotificationSummaries.forDisplay(n.type,n.summary),n.link,n.readAt,n.createdAt)).toList(),page,size,more);
    }
    @Transactional(readOnly=true,timeout=5)
    public long unread(Actor actor) {return repository.unread(access.self(actor));}
    public void read(Actor actor,UUID id) {UUID owner=access.self(actor);if(!repository.read(id,owner,clock.instant()))throw access.denied(actor,id);notifyChanged(owner);}
    public void readAll(Actor actor) {UUID owner=access.self(actor);repository.readAll(owner,clock.instant());notifyChanged(owner);}
    public String authorizedChannel(Actor actor) {return channel(access.self(actor));}
    public static String channel(UUID id) {return "notifications:account:"+id;}
    private void notifyChanged(UUID owner) {afterCommit(()->{try{sse.publish(channel(owner),Map.of("type","notification-changed"));}catch(RuntimeException ignored){/* Refresh fallback. */}});}
    private void afterCommit(Runnable work) {
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){work.run();}});
        else work.run();
    }
}
