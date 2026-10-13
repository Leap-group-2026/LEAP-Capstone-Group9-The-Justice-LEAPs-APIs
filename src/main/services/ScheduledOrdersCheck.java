package services;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import repos.OrdersRepo;

// The safety net behind Kafka: fills orders placed outside market hours and any whose event was lost
@Service
public class ScheduledOrdersCheck {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledOrdersCheck.class);

    private final OrdersRepo ordersRepo;
    private final OrderProcessingService orderProcessingService;
    private final Duration fillDelay;

    public ScheduledOrdersCheck(OrdersRepo ordersRepo,
                                OrderProcessingService orderProcessingService,
                                @Value(OrderProcessingService.FILL_DELAY) Duration fillDelay) {
        this.ordersRepo = ordersRepo;
        this.orderProcessingService = orderProcessingService;
        this.fillDelay = fillDelay;
    }

    @Scheduled(fixedRate = 300000)
    public void processOrders() {
        List<Integer> pendingIds = ordersRepo.findPendingOrderIds(fillDelay.toSeconds());

        for (Integer orderId : pendingIds) {
            try {
                orderProcessingService.process(orderId);
            } catch (RuntimeException ex) {
                // One bad order must not stop the rest of the run
                logger.error("Order {} failed during scheduled processing; marking it FAILED", orderId, ex);
                orderProcessingService.markFailed(orderId);
            }
        }
    }
}
