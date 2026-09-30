package main.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import main.entities.AccountsEntity;
import main.entities.CurrentPriceEntity;
import main.entities.OrderEntity;
import main.entities.PositionsEntity;
import main.repos.AccountsRepo;
import main.repos.CurrentPriceRepo;
import main.repos.OrdersRepo;
import main.repos.PositionsRepo;
import main.repos.TransactionsRepo;
import main.services.calculation.OrderPriceCalculator;

@Service
public class OrderProcessingService {
    private static final BigDecimal MAX_SINGLE_BUY = new BigDecimal("1000000");
    private static final BigDecimal MAX_DAILY_BUY = new BigDecimal("3000000");
    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private final OrdersRepo ordersRepo;
    private final AccountsRepo accountsRepo;
    private final TransactionsRepo transactionsRepo;
    private final PositionsRepo positionsRepo;
    private final CurrentPriceRepo currentPriceRepo;
    private final HistoricalOrdersService historicalOrdersService;
    private final Clock clock;

    public OrderProcessingService(OrdersRepo ordersRepo,
                                  AccountsRepo accountsRepo,
                                  TransactionsRepo transactionsRepo,
                                  PositionsRepo positionsRepo,
                                  CurrentPriceRepo currentPriceRepo,
                                  HistoricalOrdersService historicalOrdersService,
                                  Clock clock) {
        this.ordersRepo = ordersRepo;
        this.accountsRepo = accountsRepo;
        this.transactionsRepo = transactionsRepo;
        this.positionsRepo = positionsRepo;
        this.currentPriceRepo = currentPriceRepo;
        this.historicalOrdersService = historicalOrdersService;
        this.clock = clock;
    }

    public boolean isMarketOpen() {
        LocalTime easternTime = ZonedDateTime.now(clock)
                .withZoneSameInstant(EASTERN_ZONE)
                .toLocalTime();

        return !easternTime.isBefore(MARKET_OPEN) && !easternTime.isAfter(MARKET_CLOSE);
    }

    @Transactional
    public void process(Integer orderId) {
        if (!isMarketOpen()) {
            return;
        }

        OrderEntity order = ordersRepo.findByIdForUpdate(orderId).orElse(null);

        if (order == null || !"PENDING".equals(order.getStatus())) {
            return;
        }

        ZonedDateTime nowEastern = ZonedDateTime.now(clock).withZoneSameInstant(EASTERN_ZONE);
        LocalDateTime happenedAt =  nowEastern.toLocalDateTime();

        Integer accountId = order.getAccountId().getAccountId();
        Integer instrumentId = order.getInstrumentId().getInstrumentId();

        AccountsEntity account = accountsRepo.findById(accountId).orElse(null);
        Optional<CurrentPriceEntity> currentPrice = currentPriceRepo.findByInstrumentId(instrumentId);

        if (account == null || currentPrice.isEmpty() || !account.getAccountActive()) {
            decline(order, order.getTotalPrice(), happenedAt);
        }

        BigDecimal executionTotal = OrderPriceCalculator.calculateExecutionTotal(
                currentPrice.get().getPrice(),
                order.getQuantity()
        );

        if ("BUY".equals(order.getSide()))
            processBuy(order, account, executionTotal, nowEastern, happenedAt);
        else if ("SELL".equals(order.getSide()))
            processSell(order, account, executionTotal, happenedAt);
        else
            decline(order, executionTotal, happenedAt);
    }

    private void processBuy(OrderEntity order, AccountsEntity account, BigDecimal executionTotal, ZonedDateTime nowEastern, LocalDateTime happenedAt) {
        if (executionTotal.compareTo(MAX_SINGLE_BUY) > 0 || account.getBalance().compareTo(executionTotal) < 0) {
            decline(order, executionTotal, happenedAt);
            return;
        }

        LocalDate easternDate = nowEastern.toLocalDate();
        LocalDateTime dayStart = easternDate.atStartOfDay();
        LocalDateTime nextDayStart = easternDate.plusDays(1).atStartOfDay();

        BigDecimal dailyBuyTotal = transactionsRepo.sumExecutedBuys(account.getAccountId(), dayStart, nextDayStart);

        if (dailyBuyTotal.add(executionTotal).compareTo(MAX_DAILY_BUY) > 0) {
            decline(order, executionTotal, happenedAt);
            return;
        }

        Integer instrumentId = order.getInstrumentId().getInstrumentId();
        Optional<PositionsEntity> existingPosition = positionsRepo.findOpenForUpdate(account.getAccountId(), instrumentId);

        if (existingPosition.isEmpty()) {
            PositionsEntity position = existingPosition.get();

            int newQuantity = position.getQuantity() + order.getQuantity();
            BigDecimal newTotal = position.getTotalPrice().add(executionTotal);
            BigDecimal newAverage = newTotal.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);


            positionsRepo.update(
                position.getPositionId(),
                account.getAccountId(),
                instrumentId,
                newQuantity,
                newTotal,
                newAverage,
                position.getOpenedAt(),
                null
            );
        }
        else {
            BigDecimal averagePrice = executionTotal.divide(BigDecimal.valueOf(order.getQuantity()), 4, RoundingMode.HALF_UP);

            positionsRepo.insert(
                account.getAccountId(),
                instrumentId,
                order.getQuantity(),
                executionTotal,
                averagePrice,
                happenedAt,
                null
            );
        }

        transactionsRepo.insert(
            executionTotal, "OUT", account.getAccountId(), "TRADE", happenedAt
        );

        accountsRepo.updateBalance(account.getAccountId(), account.getBalance().subtract(executionTotal));

        fill(order, executionTotal, happenedAt);
    }

    private void processSell(OrderEntity order, AccountsEntity account, BigDecimal executionTotal, LocalDateTime happenedAt) {

    }

    private void fill(OrderEntity order, BigDecimal executionTotal, LocalDateTime happenedAt) {
        transition(order, executionTotal, "FILLED", happenedAt);
    }

    private void decline(OrderEntity order, BigDecimal executionTotal, LocalDateTime happenedAt) {
        transition(order, executionTotal, "DECLINED", happenedAt);
    }

    private void transition(OrderEntity order, BigDecimal executionTotal, String status, LocalDateTime happenedAt) {
        order.setTotalPrice(executionTotal);
        order.setStatus(status);
        order.setUpdatedAt(happenedAt);

        ordersRepo.updateExecutionOutcome(
                order.getOrderId(),
                executionTotal,
                status,
                happenedAt
        );

        historicalOrdersService.captureOrderSnapshot(order, happenedAt);
    }
}
