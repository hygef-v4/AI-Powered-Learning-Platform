package edu.aiplatform.jobs.port;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}
