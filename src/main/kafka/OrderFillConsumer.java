package kafka;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import events.OrderSubmittedEvent;
import jakarta.annotation.PreDestroy;
import services.OrderProcessingService;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderFillConsumer {
    private static final Logger logger = LoggerFactory.getLogger(OrderFillConsumer.class);

    private final OrderProcessingService orderProcessingService;
    private final Duration fillDelay;
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    public OrderFillConsumer(OrderProcessingService orderProcessingService,
                             @Value(OrderProcessingService.FILL_DELAY) Duration fillDelay,
                             @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.orderProcessingService = orderProcessingService;
        this.fillDelay = fillDelay;
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("order-fill-");
        scheduler.initialize();
        logger.info("OrderFillConsumer initialized: bootstrapServers={}, fillDelay={}", bootstrapServers, fillDelay);
    }


    @KafkaListener(topics = "${app.kafka.topic.order-submitted}", groupId = "order-fill", concurrency = "3")
    public void onOrderSubmitted(OrderSubmittedEvent event, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        logger.info("Received OrderSubmittedEvent orderId={} partition={} offset={}", event.orderId(), partition, offset);
        scheduler.schedule(() -> fill(event.orderId()), scheduler.getClock().instant().plus(fillDelay));
        logger.info("Scheduled fill for orderId={} in {}", event.orderId(), fillDelay);
    }

    private void fill(Integer orderId) {
        try {
            orderProcessingService.process(orderId);
        } catch (RuntimeException ex) {
            logger.error("Order {} failed during its delayed fill; the sweeper will retry it", orderId, ex);
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdown();
    }
}
