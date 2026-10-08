package kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import events.OrderSubmittedEvent;
import services.OrderProcessingService;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderFillConsumer {
    private final OrderProcessingService orderProcessingService;

    public OrderFillConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }


    @KafkaListener(topics = "${app.kafka.topic.order-submitted}", groupId = "order-fill", concurrency = "3")
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        orderProcessingService.process(event.orderId());
    }
}
