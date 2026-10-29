package edu.aiplatform.notifications.infrastructure;
import edu.aiplatform.notifications.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepositoryAdapter implements INotificationRepository {
    private final EntityManager em;
    public NotificationRepositoryAdapter(EntityManager em) {this.em=em;}
    public boolean insert(Notification n) {
        return em.createNativeQuery("insert into notifications(id,recipient_id,type,source_event_id,title,summary,link,created_at) values (:id,:recipient,:type,:event,:title,:summary,:link,:now) on conflict(source_event_id,recipient_id,type) do nothing")
                .setParameter("id",n.id).setParameter("recipient",n.recipientId).setParameter("type",n.type.name()).setParameter("event",n.sourceEventId)
                .setParameter("title",n.title).setParameter("summary",n.summary).setParameter("link",n.link).setParameter("now",n.createdAt).executeUpdate()==1;
    }
    public Optional<Notification> find(UUID id) {return Optional.ofNullable(em.find(Notification.class,id));}
    public List<Notification> list(UUID recipient,boolean unread,int page,int size) {
        return em.createQuery("select n from Notification n where n.recipientId=:recipient"+(unread?" and n.readAt is null":"")+" order by n.createdAt desc,n.id desc",Notification.class)
                .setParameter("recipient",recipient).setFirstResult((page-1)*size).setMaxResults(size).getResultList();
    }
    public long unread(UUID recipient) {return em.createQuery("select count(n) from Notification n where n.recipientId=:id and n.readAt is null",Long.class).setParameter("id",recipient).getSingleResult();}
    public boolean read(UUID id,UUID recipient,Instant now) {
        var n=find(id);if(n.isEmpty()||!n.get().recipientId.equals(recipient))return false;
        em.createQuery("update Notification n set n.readAt=:now where n.id=:id and n.recipientId=:recipient and n.readAt is null")
                .setParameter("now",now).setParameter("id",id).setParameter("recipient",recipient).executeUpdate();return true;
    }
    public void readAll(UUID recipient,Instant now) {em.createQuery("update Notification n set n.readAt=:now where n.recipientId=:recipient and n.readAt is null").setParameter("now",now).setParameter("recipient",recipient).executeUpdate();}
    public Map<String,Boolean> preferences(UUID account) {
        Map<String,Boolean> result=new HashMap<>();em.createQuery("select p from NotificationPreference p where p.accountId=:id",NotificationPreference.class)
                .setParameter("id",account).getResultList().forEach(p->result.put(p.type,p.emailEnabled));return result;
    }
    public void preference(UUID account,String type,boolean enabled) {
        em.createNativeQuery("insert into notification_preferences(account_id,type,email_enabled) values (:id,:type,:enabled) on conflict(account_id,type) do update set email_enabled=excluded.email_enabled")
                .setParameter("id",account).setParameter("type",type).setParameter("enabled",enabled).executeUpdate();
    }
    public void addEmail(EmailDelivery delivery) {em.persist(delivery);}
    public Optional<EmailDelivery> lockEmail(UUID id) {return Optional.ofNullable(em.find(EmailDelivery.class,id,LockModeType.PESSIMISTIC_WRITE,Map.of("jakarta.persistence.lock.timeout",2000)));}
    public List<EmailDelivery> pending(Instant olderThan) {
        return em.createQuery("select e from EmailDelivery e where e.status=:status and e.scheduledFor<:time order by e.scheduledFor,e.id",EmailDelivery.class)
                .setParameter("status",EmailDelivery.Status.PENDING).setParameter("time",olderThan).setMaxResults(100).getResultList();
    }
    public List<EmailDelivery> deferred(Instant now) {
        return em.createQuery("select e from EmailDelivery e where e.status=:status and e.scheduledFor<=:now order by e.priority desc,e.createdAt,e.id",EmailDelivery.class)
                .setParameter("status",EmailDelivery.Status.DEFERRED).setParameter("now",now).setMaxResults(300).getResultList();
    }
    public long reserved(Instant from,Instant to) {
        return em.createQuery("select count(e) from EmailDelivery e where e.status in :statuses and e.scheduledFor>=:from and e.scheduledFor<:to",Long.class)
                .setParameter("statuses",List.of(EmailDelivery.Status.PENDING,EmailDelivery.Status.SENT)).setParameter("from",from).setParameter("to",to).getSingleResult();
    }
    public void lockBudget() {
        em.createNativeQuery("select set_config('lock_timeout','2s',true)").getSingleResult();
        em.createNativeQuery("select 1 from pg_advisory_xact_lock(14002000)").getSingleResult();
    }
    public boolean reminder(UUID assignment,UUID classId,Instant closesAt,String status,Instant now) {
        return em.createNativeQuery("insert into deadline_reminders(assignment_id,class_id,closes_at,status,processed_at) values (:assignment,:class,:closes,:status,:now) on conflict do nothing")
                .setParameter("assignment",assignment).setParameter("class",classId).setParameter("closes",closesAt).setParameter("status",status).setParameter("now",now).executeUpdate()==1;
    }
    public int deleteOld(Instant cutoff) {
        return em.createNativeQuery("delete from notifications where id in (select id from notifications where created_at<:cutoff order by created_at,id limit 10000)")
                .setParameter("cutoff",cutoff).executeUpdate();
    }
}
