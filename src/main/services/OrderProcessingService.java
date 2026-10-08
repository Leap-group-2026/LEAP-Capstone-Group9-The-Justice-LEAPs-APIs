package services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import entities.AccountsEntity;
import entities.CurrentPriceEntity;
import entities.OrderEntity;
import entities.PositionsEntity;
import repos.AccountsRepo;
import repos.CurrentPriceRepo;
import repos.OrdersRepo;
import repos.PositionsRepo;
import services.calculation.OrderPriceCalculator;

@Service
public class OrderProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);
    public static final String FILL_DELAY = "${app.orders.fill-delay:5s}";
    private static final BigDecimal MAX_SINGLE_BUY = new BigDecimal("1000000");
    private static final BigDecimal MAX_DAILY_BUY = new BigDecimal("3000000");
    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private final OrdersRepo ordersRepo;
    private final AccountsRepo accountsRepo;
    private final PositionsRepo positionsRepo;
    private final CurrentPriceRepo currentPriceRepo;
    private final HistoricalOrdersService historicalOrdersService;
    private final Clock clock;

    public OrderProcessingService(OrdersRepo ordersRepo,
                                  AccountsRepo accountsRepo,
                                  PositionsRepo positionsRepo,
                                  CurrentPriceRepo currentPriceRepo,
                                  HistoricalOrdersService historicalOrdersService,
                                  Clock clock) {
        this.ordersRepo = ordersRepo;
        this.accountsRepo = accountsRepo;
        this.positionsRepo = positionsRepo;
        this.currentPriceRepo = currentPriceRepo;
        this.historicalOrdersService = historicalOrdersService;
        this.clock = clock;
    }

    public boolean isMarketOpen() {
        ZonedDateTime nowEastern = ZonedDateTime.now(clock).withZoneSameInstant(EASTERN_ZONE);
        DayOfWeek day = nowEastern.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime easternTime = nowEastern.toLocalTime();
        return !easternTime.isBefore(MARKET_OPEN) && !easternTime.isAfter(MARKET_CLOSE);
    }

    @Transactional
    public void process(Integer orderId) {
        logger.info("Starting order processing for orderId={}", orderId);

        if (!isMarketOpen()) {
            logger.info("Skipping order processing for orderId={} because the market is closed", orderId);
            return;
        }

        OrderEntity order = ordersRepo.findByIdForUpdate(orderId).orElse(null);

        if (order == null) {
            logger.warn("Skipping order processing because orderId={} was not found", orderId);
            return;
        }

        if (!"PENDING".equals(order.getStatus())) {
            logger.info("Skipping order processing for orderId={} because status={} is not PENDING", orderId, order.getStatus());
            return;
        }

        logger.info("Loaded pending order orderId={}, side={}, quantity={}", order.getOrderId(), order.getSide(), order.getQuantity());

        ZonedDateTime nowEastern = ZonedDateTime.now(clock).withZoneSameInstant(EASTERN_ZONE);
        LocalDateTime happenedAt =  nowEastern.toLocalDateTime();

        Integer accountId = order.getAccountId().getAccountId();
        Integer instrumentId = order.getInstrumentId().getInstrumentId();

        AccountsEntity account = accountsRepo.findByIdForUpdate(accountId).orElse(null);
        Optional<CurrentPriceEntity> currentPrice = currentPriceRepo.findByInstrumentId(instrumentId);

        logger.info("Fetched processing dependencies for orderId={}: accountFound={}, currentPriceFound={}",
            order.getOrderId(),
            account != null,
            currentPrice.isPresent());

        if (account == null) {
            decline(order, order.getTotalPrice(), happenedAt, "account was not found");
            return;
        }

        if (currentPrice.isEmpty()) {
            decline(order, order.getTotalPrice(), happenedAt, "current price was not available");
            return;
        }

        if (!account.getAccountActive()) {
            decline(order, order.getTotalPrice(), happenedAt, "account is inactive");
            return;
        }

        BigDecimal executionTotal = OrderPriceCalculator.calculateExecutionTotal(
                currentPrice.get().getPrice(),
                order.getQuantity()
        );

        logger.info("Calculated execution total for orderId={}: marketPrice={}, executionTotal={}",
            order.getOrderId(),
            currentPrice.get().getPrice(),
            executionTotal);

        if ("BUY".equals(order.getSide()))
            processBuy(order, account, executionTotal, nowEastern, happenedAt);
        else if ("SELL".equals(order.getSide()))
            processSell(order, account, executionTotal, happenedAt);
        else
            decline(order, executionTotal, happenedAt, "order side is unsupported");
    }

    private void processBuy(OrderEntity order, AccountsEntity account, BigDecimal executionTotal, ZonedDateTime nowEastern, LocalDateTime happenedAt) {
        logger.info("Processing BUY order orderId={} for accountId={} with executionTotal={}",
            order.getOrderId(),
            account.getAccountId(),
            executionTotal);

        if (executionTotal.compareTo(MAX_SINGLE_BUY) > 0 || account.getBalance().compareTo(executionTotal) < 0) {
            decline(order, executionTotal, happenedAt, "buy order exceeded single-order limit or available balance");
            return;
        }

        LocalDate easternDate = nowEastern.toLocalDate();
        LocalDateTime dayStart = easternDate.atStartOfDay();
        LocalDateTime nextDayStart = easternDate.plusDays(1).atStartOfDay();

        BigDecimal dailyBuyTotal = ordersRepo.sumFilledBuys(account.getAccountId(), dayStart, nextDayStart);

        logger.info("Calculated daily BUY exposure for orderId={}: currentDailyTotal={}, proposedDailyTotal={}",
            order.getOrderId(),
            dailyBuyTotal,
            dailyBuyTotal.add(executionTotal));

        if (dailyBuyTotal.add(executionTotal).compareTo(MAX_DAILY_BUY) > 0) {
            decline(order, executionTotal, happenedAt, "buy order exceeded the daily buy limit");
            return;
        }

        Integer instrumentId = order.getInstrumentId().getInstrumentId();
        Optional<PositionsEntity> existingPosition = positionsRepo.findOpenForUpdate(account.getAccountId(), instrumentId);

        if (existingPosition.isPresent()) {
            PositionsEntity position = existingPosition.get();

            int newQuantity = position.getQuantity() + order.getQuantity();
            BigDecimal newTotal = position.getTotalPrice().add(executionTotal);
            BigDecimal newAverage = newTotal.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);

            logger.info("Updating existing position for BUY orderId={}: positionId={}, newQuantity={}, newAveragePrice={}",
                order.getOrderId(),
                position.getPositionId(),
                newQuantity,
                newAverage);

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

            logger.info("Creating new position for BUY orderId={}: instrumentId={}, quantity={}, averagePrice={}",
                order.getOrderId(),
                instrumentId,
                order.getQuantity(),
                averagePrice);

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

        accountsRepo.updateBalance(account.getAccountId(), account.getBalance().subtract(executionTotal));

        logger.info("Completed BUY account updates for orderId={}, newBalance={}",
            order.getOrderId(),
            account.getBalance().subtract(executionTotal));

        fill(order, executionTotal, happenedAt);
    }

    private void processSell(OrderEntity order, AccountsEntity account, BigDecimal executionTotal, LocalDateTime happenedAt) {
        logger.info("Processing SELL order orderId={} for accountId={} with executionTotal={}",
            order.getOrderId(),
            account.getAccountId(),
            executionTotal);

        Integer instrumentId = order.getInstrumentId().getInstrumentId();
        Optional<PositionsEntity> existingPosition = positionsRepo.findOpenForUpdate(account.getAccountId(), instrumentId);

        if (existingPosition.isEmpty()) {
            decline(order, executionTotal, happenedAt, "no open position was found to sell");
            return;
        }

        PositionsEntity position = existingPosition.get();

        if (position.getQuantity() == null || position.getAveragePrice() == null || position.getTotalPrice() == null
                || position.getQuantity() < order.getQuantity()) {
            decline(order, executionTotal, happenedAt, "position quantity or cost basis could not support the sell order");
            return;
        }

        int remainingQuantity = position.getQuantity() - order.getQuantity();

        if (remainingQuantity == 0) {
            logger.info("Closing position for SELL orderId={}: positionId={}", order.getOrderId(), position.getPositionId());
            positionsRepo.update(
                position.getPositionId(),
                account.getAccountId(),
                instrumentId,
                0,
                BigDecimal.ZERO,
                position.getAveragePrice(),
                position.getOpenedAt(),
                happenedAt
            );
        }
        else {
            BigDecimal quantitySold = BigDecimal.valueOf(order.getQuantity());
            BigDecimal reducedCostBasis = position.getAveragePrice().multiply(quantitySold);
            BigDecimal remainingTotal = position.getTotalPrice().subtract(reducedCostBasis);

            if (remainingTotal.compareTo(BigDecimal.ZERO) < 0) {
                remainingTotal = BigDecimal.ZERO;
            }

            logger.info("Reducing position for SELL orderId={}: positionId={}, remainingQuantity={}, remainingTotal={}",
                order.getOrderId(),
                position.getPositionId(),
                remainingQuantity,
                remainingTotal);

            positionsRepo.update(
                position.getPositionId(),
                account.getAccountId(),
                instrumentId,
                remainingQuantity,
                remainingTotal,
                position.getAveragePrice(),
                position.getOpenedAt(),
                null
            );
        }

        accountsRepo.updateBalance(account.getAccountId(), account.getBalance().add(executionTotal));

        logger.info("Completed SELL account updates for orderId={}, newBalance={}",
            order.getOrderId(),
            account.getBalance().add(executionTotal));

        fill(order, executionTotal, happenedAt);
    }


    @Transactional
    public void markFailed(Integer orderId) {
        OrderEntity order = ordersRepo.findByIdForUpdate(orderId).orElse(null);

        if (order == null || !"PENDING".equals(order.getStatus())) {
            return;
        }

        LocalDateTime failedAt = ZonedDateTime.now(clock).withZoneSameInstant(EASTERN_ZONE).toLocalDateTime();
        transition(order, order.getTotalPrice(), "FAILED", failedAt);
    }

    private void fill(OrderEntity order, BigDecimal executionTotal, LocalDateTime happenedAt) {
        logger.info("Marking orderId={} as FILLED with executionTotal={}", order.getOrderId(), executionTotal);
        transition(order, executionTotal, "FILLED", happenedAt);
    }

    private void decline(OrderEntity order, BigDecimal executionTotal, LocalDateTime happenedAt, String reason) {
        logger.warn("Declining orderId={} with executionTotal={}: {}", order.getOrderId(), executionTotal, reason);
        transition(order, executionTotal, "DECLINED", happenedAt);
    }

    private void transition(OrderEntity order, BigDecimal executionTotal, String status, LocalDateTime happenedAt) {
        logger.info("Persisting order state transition for orderId={}: status={}, happenedAt={}",
            order.getOrderId(),
            status,
            happenedAt);
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
        logger.info("Captured historical snapshot for orderId={} after transition to {}", order.getOrderId(), status);
    }
}
