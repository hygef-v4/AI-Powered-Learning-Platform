package edu.aiplatform.notifications.service;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class EmailSendJobHandler implements IJobHandler,IPendingSweeper {
    private final EmailDeliveryService delivery;private final INotificationRepository repository;
    public EmailSendJobHandler(EmailDeliveryService delivery,INotificationRepository repository) {this.delivery=delivery;this.repository=repository;}
    public String jobType() {return JobTypes.EMAIL_SEND;}
    public JobQueue queue() {return JobQueue.EMAIL;}
    public Duration timeout() {return Duration.ofSeconds(35);}
    public void handle(JobMessage message) {
        UUID deliveryId=id(message);
        try {delivery.send(deliveryId);}catch(RetryableJobException retry){throw retry;}
        catch(RuntimeException unavailable){throw new RetryableJobException("NOTIFICATION_DELIVERY_UNAVAILABLE");}
    }
    public void onFailed(JobMessage message,String ignored) {delivery.failed(id(message));}
    @Transactional(readOnly=true,timeout=5)
    public List<PendingJob> findStalePending(Instant olderThan) {return repository.pending(olderThan).stream().map(e->new PendingJob("email:"+e.id,Map.of("deliveryId",e.id.toString()))).toList();}
    private UUID id(JobMessage m) {return UUID.fromString(Objects.toString(m.payload().get("deliveryId")));}
}
