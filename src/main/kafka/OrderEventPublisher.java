package main.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import main.events.OrderSubmittedEvent;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderSubmittedEvent> kafkaTemplate;
    private final String topic;

    public OrderEventPublisher(KafkaTemplate<String, OrderSubmittedEvent> kafkaTemplate,
                               @Value("${app.kafka.topic.order-submitted}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.accountId()), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Order {} committed but its event was not published; the sweeper will pick it up",
                        event.orderId(), ex);
                }
            });
    }
}
