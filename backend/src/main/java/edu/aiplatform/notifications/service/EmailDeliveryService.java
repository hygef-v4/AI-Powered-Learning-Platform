package edu.aiplatform.notifications.service;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import edu.aiplatform.assignments.contract.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(timeout=20)
public class EmailDeliveryService {
    private final INotificationRepository repository;private final EmailCapPolicy cap;private final IJob jobs;
    private final IAccountLookup accounts;private final IMail mail;private final Clock clock;private final String baseUrl;
    private final IAssignmentQuery assignments;
    public EmailDeliveryService(INotificationRepository repository,EmailCapPolicy cap,IJob jobs,IAccountLookup accounts,IMail mail,Clock clock,IAssignmentQuery assignments,@Value("${APP_PUBLIC_URL:http://localhost:3000}") String baseUrl) {
        var uri=URI.create(baseUrl);if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException("Invalid U14 public base URL");
        this.repository=repository;this.cap=cap;this.jobs=jobs;this.accounts=accounts;this.mail=mail;this.clock=clock;this.assignments=assignments;this.baseUrl=baseUrl.replaceAll("/+$","");
    }
    public void schedule(Notification n,String type,Instant expiresAt) {
        if(type==null)return;Instant now=clock.instant();var e=new EmailDelivery();e.id=UUID.randomUUID();e.notificationId=n.id;e.recipientId=n.recipientId;e.type=type;
        e.priority=EmailCapPolicy.PRIORITY.get(type);e.createdAt=now;e.scheduledFor=now;e.expiresAt=expiresAt;
        if(!type.equals("BACKUP_FAILED")&&!repository.preferences(n.recipientId).getOrDefault(type,true))e.status=EmailDelivery.Status.SKIPPED;
        else if(expired(e,now))e.status=EmailDelivery.Status.SKIPPED;
        else if(cap.available(now))e.status=EmailDelivery.Status.PENDING;
        else {e.status=EmailDelivery.Status.DEFERRED;e.scheduledFor=cap.nextDay(now);}
        repository.addEmail(e);if(e.status==EmailDelivery.Status.PENDING)enqueue(e);
    }
    @Transactional(timeout=30,noRollbackFor=RetryableJobException.class)
    public void send(UUID id) {
        repository.lockBudget();
        var e=repository.lockEmail(id).orElse(null);if(e==null||e.status!=EmailDelivery.Status.PENDING)return;
        Instant now=clock.instant();if(expired(e,now)){e.status=EmailDelivery.Status.SKIPPED;return;}
        if(!cap.sameDay(e.scheduledFor,now)) {
            if(!cap.available(now)){e.status=EmailDelivery.Status.DEFERRED;e.scheduledFor=cap.nextDay(now);return;}e.scheduledFor=now;
        }
        AccountContact contact;
        try {contact=accounts.getContact(e.recipientId).orElse(null);}catch(RuntimeException unavailable){throw new RetryableJobException("NOTIFICATION_ACCOUNT_UNAVAILABLE");}
        if(contact==null||contact.status()!=AccountStatus.ACTIVE||(e.type.equals("BACKUP_FAILED")?contact.role()!=Role.ADMIN:contact.role()!=Role.STUDENT)) {e.status=EmailDelivery.Status.SKIPPED;return;}
        var n=repository.find(e.notificationId).orElse(null);if(n==null){e.status=EmailDelivery.Status.SKIPPED;return;}
        if(e.type.equals("DEADLINE_REMINDER")) {
            UUID assignmentId;
            try {assignmentId=AssignmentNotificationLink.assignmentId(n.link);}
            catch(IllegalArgumentException invalidLink){e.status=EmailDelivery.Status.SKIPPED;e.lastError="INVALID_ASSIGNMENT_LINK";return;}
            AssignmentView a;
            try {a=assignments.get(assignmentId).orElse(null);}
            catch(RuntimeException unavailable){throw new RetryableJobException("NOTIFICATION_ASSIGNMENT_UNAVAILABLE");}
            if(a==null||a.status()!=AssignmentStatus.OPEN||!Objects.equals(a.closesAt(),e.expiresAt)){e.status=EmailDelivery.Status.SKIPPED;return;}
        }
        e.attempts++;
        // Provider acceptance and DB commit cannot be exactly-once without provider idempotency support.
        try {mail.send(new MailMessage(contact.email(),n.title,"Bạn có thông báo mới trên nền tảng học tập. Đăng nhập để xem: "+baseUrl+n.link,null));}
        catch(RuntimeException failure) {throw new RetryableJobException("NOTIFICATION_MAIL_UNAVAILABLE");}
        e.status=EmailDelivery.Status.SENT;e.sentAt=now;e.lastError=null;
    }
    public void failed(UUID id) {repository.lockEmail(id).filter(e->e.status==EmailDelivery.Status.PENDING).ifPresent(e->{e.status=EmailDelivery.Status.FAILED;e.attempts=Math.max(e.attempts,5);e.lastError="MAIL_RETRIES_EXHAUSTED";});}
    public void releaseDeferred(Instant now) {
        // All paths take the budget lock before row locks to avoid deadlocks across midnight.
        repository.lockBudget();
        for(var candidate:repository.deferred(now).stream().sorted(Comparator.comparingInt((EmailDelivery e)->e.priority).reversed()).toList()) {
            var e=repository.lockEmail(candidate.id).orElse(null);if(e==null||e.status!=EmailDelivery.Status.DEFERRED)continue;
            if(expired(e,now)){e.status=EmailDelivery.Status.SKIPPED;continue;}
            if(!cap.available(now))break;e.status=EmailDelivery.Status.PENDING;e.scheduledFor=now;enqueue(e);
        }
    }
    private boolean expired(EmailDelivery e,Instant now) {return e.expiresAt!=null&&!e.expiresAt.isAfter(now);}
    private void enqueue(EmailDelivery e) {jobs.enqueue(JobTypes.EMAIL_SEND,Map.of("deliveryId",e.id.toString()),"email:"+e.id);}
}
