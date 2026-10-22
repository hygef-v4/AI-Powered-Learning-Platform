package edu.aiplatform.notifications.infrastructure;
import edu.aiplatform.notifications.application.NotificationEventService;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;
import java.util.*;
@Configuration
public class NotificationTopology {
    public static final String QUEUE="jobs.notification";
    public static final int[] RETRY_MS={30000,60000,120000,240000,480000};
    @Bean
    Declarables notificationDeclarables() {
        List<Declarable> resources=new ArrayList<>();var events=new TopicExchange("platform.events",true,false);
        var failed=new FanoutExchange("notifications.failed",true,false);var dead=QueueBuilder.durable("notifications.failed").build();
        var queue=QueueBuilder.durable(QUEUE).deadLetterExchange(failed.getName()).build();resources.add(queue);resources.add(failed);resources.add(dead);resources.add(BindingBuilder.bind(dead).to(failed));
        NotificationEventService.SOURCES.keySet().forEach(type->resources.add(BindingBuilder.bind(queue).to(events).with(type)));
        for(int i=0;i<RETRY_MS.length;i++) {
            var retry=new FanoutExchange(retryExchange(i),true,false);
            var waiting=QueueBuilder.durable(retry.getName()).ttl(RETRY_MS[i]).deadLetterExchange("platform.events").build();
            resources.add(retry);resources.add(waiting);resources.add(BindingBuilder.bind(waiting).to(retry));
        }
        return new Declarables(resources);
    }
    public static String retryExchange(int attempt) {return "notifications.retry."+attempt;}
}
