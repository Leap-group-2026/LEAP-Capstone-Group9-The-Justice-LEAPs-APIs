package main.services.validation;

import org.springframework.stereotype.Component;
import main.dto.request.CreateOrderRequest;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.exception.InvalidOrderException;
import main.services.calculation.OrderPriceCalculator;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

@Component
public class BuyOrderValidator {
    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    public void validate(CreateOrderRequest request, AccountsEntity account, InstrumentEntity instrument, ZonedDateTime submittedAt) {
        validateBalance(request, account, instrument);
        validateMarketHours(submittedAt);
    }

    private void validateBalance(CreateOrderRequest request, AccountsEntity account, InstrumentEntity instrument) {
        BigDecimal orderPrice = OrderPriceCalculator.calculateOrderPrice(instrument, request.quantity());
        BigDecimal accountBalance = account.getBalance();

        if (accountBalance.compareTo(orderPrice) < 0) {
            throw new InvalidOrderException(
                "balance",
                String.format("Insufficient balance for order: balance=$%s, required=$%s", accountBalance, orderPrice)
            );
        }
    }

    private void validateMarketHours(ZonedDateTime submittedAt) {
        if (!isMarketOpen(submittedAt)) {
            throw new InvalidOrderException(
                "market_hours",
                "Market is not open. Submit orders between 9:30 AM - 4:00 PM ET"
            );
        }
    }

    private boolean isMarketOpen(ZonedDateTime submittedAt) {
        ZonedDateTime etTime = submittedAt.withZoneSameInstant(EASTERN_ZONE);
        LocalTime orderTime = etTime.toLocalTime();

        boolean isWithinMarketHours = !orderTime.isBefore(MARKET_OPEN) && !orderTime.isAfter(MARKET_CLOSE);

        return isWithinMarketHours;
    }
}
