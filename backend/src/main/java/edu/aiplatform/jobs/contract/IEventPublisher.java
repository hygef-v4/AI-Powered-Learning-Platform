package edu.aiplatform.jobs.contract;

/** Phát sự kiện thông báo sau commit, không đảm bảo giao hàng. */
public interface IEventPublisher {

    void publish(DomainEvent event);
}
