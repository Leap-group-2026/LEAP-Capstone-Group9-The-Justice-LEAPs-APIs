package main.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import main.entities.OrderEntity;
import main.exception.TransactionProcessingException;
import main.repos.OrdersRepo;

@Service
public class ScheduledOrdersCheck {
    private final OrdersRepo ordersRepo;
    private final OrderProcessingService orderProcessingService;
    private final HistoricalOrdersService historicalOrdersService;

    public ScheduledOrdersCheck(OrdersRepo ordersRepo,
                                OrderProcessingService orderProcessingService,
                                HistoricalOrdersService historicalOrdersService) {
        this.ordersRepo = ordersRepo;
        this.orderProcessingService = orderProcessingService;
        this.historicalOrdersService = historicalOrdersService;
    }

    @Scheduled(fixedRate = 300000)
    public void processOrders() {
        List<Integer> pendingIds = ordersRepo.findPendingOrderIds();

        for (Integer orderId : pendingIds) {
            try {
                orderProcessingService.process(orderId);
            } catch (RuntimeException ex) {
                failOrderWithSnapshot(orderId);

                String reason = ex.getMessage() == null
                    ? "Unexpected runtime exception during scheduled order processing"
                    : ex.getMessage();

                throw new TransactionProcessingException(orderId, reason, ex);
            }
        }
    }

    private void failOrderWithSnapshot(Integer orderId) {
        OrderEntity order = ordersRepo.findByIdForUpdate(orderId).orElse(null);

        if (order == null) {
            return;
        }

        LocalDateTime failedAt = LocalDateTime.now();
        order.setStatus("FAILED");
        order.setUpdatedAt(failedAt);

        ordersRepo.updateExecutionOutcome(
            orderId,
            order.getTotalPrice(),
            "FAILED",
            failedAt
        );

        historicalOrdersService.captureOrderSnapshot(order, failedAt);
    }
}
