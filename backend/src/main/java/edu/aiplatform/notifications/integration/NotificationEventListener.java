package edu.aiplatform.notifications.integration;

import edu.aiplatform.notifications.config.NotificationTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.notifications.service.NotificationEventService;
import java.io.IOException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component @Profile("worker")
public class NotificationEventListener {
    private final NotificationEventService service;private final ObjectMapper json;private final RabbitTemplate rabbit;
    public NotificationEventListener(NotificationEventService service,ObjectMapper json,RabbitTemplate rabbit) {this.service=service;this.json=json;this.rabbit=rabbit;}
    @RabbitListener(queues=NotificationTopology.QUEUE,ackMode="MANUAL",concurrency="1")
    public void receive(Message message,Channel channel) throws IOException {
        long tag=message.getMessageProperties().getDeliveryTag();DomainEvent event;
        try {
            if(message.getBody().length>262144)throw new IllegalArgumentException("Event too large");
            event=json.readValue(message.getBody(),DomainEvent.class);
            if(event==null||event.schemaVersion()!=1||event.eventType()==null||!NotificationEventService.SOURCES.containsKey(event.eventType())) {channel.basicAck(tag,false);return;}
            if(!event.eventType().equals(message.getMessageProperties().getReceivedRoutingKey()))throw new IllegalArgumentException("Event routing mismatch");
        }catch(IOException|IllegalArgumentException malformed){channel.basicReject(tag,false);return;}
        try {service.accept(event);channel.basicAck(tag,false);}
        catch(IllegalArgumentException malformed) {channel.basicReject(tag,false);}
        catch(RuntimeException unavailable) {
            Object header=message.getMessageProperties().getHeaders().get("u14-attempt");int attempt=header instanceof Number n?n.intValue():0;
            if(attempt<0||attempt>=NotificationTopology.RETRY_MS.length){channel.basicReject(tag,false);return;}
            var props=new MessageProperties();props.setContentType(MessageProperties.CONTENT_TYPE_JSON);props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);props.setHeader("u14-attempt",attempt+1);
            try {
                rabbit.invoke(operations->{operations.send(NotificationTopology.retryExchange(attempt),event.eventType(),new Message(message.getBody(),props));operations.waitForConfirmsOrDie(5000);return null;});
                channel.basicAck(tag,false);
            }catch(RuntimeException publishFailed){channel.basicNack(tag,false,true);}
        }
    }
}
