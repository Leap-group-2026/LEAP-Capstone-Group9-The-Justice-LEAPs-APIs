package services;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import dto.response.DailyMoneyMovedResponse;
import repos.OrdersRepo;

@Service
public class OrderAnalyticsService {
    // Order fills are stamped in Eastern wall-clock time, so days are counted in Eastern too
    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    static final int MAX_DAYS = 365;

    private final OrdersRepo ordersRepo;
    private final Clock clock;

    public OrderAnalyticsService(OrdersRepo ordersRepo, Clock clock) {
        this.ordersRepo = ordersRepo;
        this.clock = clock;
    }

    // Money moved by filled orders over the last `days` days, today included.
    public List<DailyMoneyMovedResponse> moneyMoved(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS);
        }
        LocalDate today = LocalDate.now(clock.withZone(EASTERN_ZONE));
        return ordersRepo.sumFilledOrdersByDay(today.minusDays(days - 1L).atStartOfDay());
    }
}
