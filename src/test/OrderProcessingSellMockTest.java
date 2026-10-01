import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import entities.AccountsEntity;
import entities.CurrentPriceEntity;
import entities.InstrumentEntity;
import entities.OrderEntity;
import entities.PositionsEntity;
import repos.AccountsRepo;
import repos.CurrentPriceRepo;
import repos.HistoricalOrdersRepo;
import repos.OrdersRepo;
import repos.PositionsRepo;
import services.HistoricalOrdersService;
import services.OrderProcessingService;

@ExtendWith(MockitoExtension.class)
public class OrderProcessingSellMockTest {

	private static final Instant MARKET_OPEN_INSTANT = Instant.parse("2026-01-05T15:00:00Z");
	private static final LocalDateTime EXPECTED_HAPPENED_AT = LocalDateTime.of(2026, 1, 5, 10, 0);

	@Mock
	private AccountsRepo accountsRepo;

	@Mock
	private OrdersRepo ordersRepo;

	@Mock
	private CurrentPriceRepo currentPriceRepo;

	@Mock
	private PositionsRepo positionsRepo;

	@Mock
	private HistoricalOrdersRepo historicalOrdersRepo;

	private OrderProcessingService orderProcessingService;
	private HistoricalOrdersService historicalOrdersService;
	private AccountsEntity account;
	private InstrumentEntity instrument;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(MARKET_OPEN_INSTANT, ZoneOffset.UTC);
		historicalOrdersService = new HistoricalOrdersService(historicalOrdersRepo, new ObjectMapper().findAndRegisterModules(), ordersRepo);
		orderProcessingService = new OrderProcessingService(
			ordersRepo,
			accountsRepo,
			positionsRepo,
			currentPriceRepo,
			historicalOrdersService,
			clock);

		account = new AccountsEntity();
		account.setAccountId(202);
		account.setBalance(new BigDecimal("500.0000"));
		account.setAccountActive(true);

		instrument = new InstrumentEntity();
		instrument.setInstrumentId(303);
	}

	@Test
	void processSellDeclinesWhenNoOpenPositionExists() {
		OrderEntity order = insertSellOrder(4);
		CurrentPriceEntity currentPrice = currentPrice("25.0000");

		when(ordersRepo.findByIdForUpdate(order.getOrderId())).thenReturn(Optional.of(order));
		when(accountsRepo.findByIdForUpdate(account.getAccountId())).thenReturn(Optional.of(account));
		when(currentPriceRepo.findByInstrumentId(instrument.getInstrumentId())).thenReturn(Optional.of(currentPrice));
		when(positionsRepo.findOpenForUpdate(account.getAccountId(), instrument.getInstrumentId())).thenReturn(Optional.empty());

		orderProcessingService.process(order.getOrderId());

		verify(ordersRepo).updateExecutionOutcome(
			order.getOrderId(),
			new BigDecimal("100.0000"),
			"DECLINED",
			EXPECTED_HAPPENED_AT);
		verify(positionsRepo, never()).update(
			org.mockito.ArgumentMatchers.anyInt(),
			org.mockito.ArgumentMatchers.anyInt(),
			org.mockito.ArgumentMatchers.anyInt(),
			org.mockito.ArgumentMatchers.anyInt(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any());
		verify(accountsRepo, never()).updateBalance(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
		verify(historicalOrdersRepo).insert(eq(order.getOrderId()), eq(account.getAccountId()), anyString(), eq(order.getCreatedAt()));
	}

	@Test
	void processSellPartiallyReducesPositionAndCreditsBalance() {
		OrderEntity order = insertSellOrder(3);
		CurrentPriceEntity currentPrice = currentPrice("30.0000");
		PositionsEntity position = position(404, 12, "240.0000", "20.0000");

		when(ordersRepo.findByIdForUpdate(order.getOrderId())).thenReturn(Optional.of(order));
		when(accountsRepo.findByIdForUpdate(account.getAccountId())).thenReturn(Optional.of(account));
		when(currentPriceRepo.findByInstrumentId(instrument.getInstrumentId())).thenReturn(Optional.of(currentPrice));
		when(positionsRepo.findOpenForUpdate(account.getAccountId(), instrument.getInstrumentId())).thenReturn(Optional.of(position));

		orderProcessingService.process(order.getOrderId());

		verify(positionsRepo).update(
			404,
			account.getAccountId(),
			instrument.getInstrumentId(),
			9,
			new BigDecimal("180.0000"),
			new BigDecimal("20.0000"),
			LocalDateTime.of(2024, 9, 1, 9, 30),
			null);
		verify(accountsRepo).updateBalance(account.getAccountId(), new BigDecimal("590.0000"));
		verify(ordersRepo).updateExecutionOutcome(
			order.getOrderId(),
			new BigDecimal("90.0000"),
			"FILLED",
			EXPECTED_HAPPENED_AT);
		verify(historicalOrdersRepo).insert(eq(order.getOrderId()), eq(account.getAccountId()), anyString(), eq(order.getCreatedAt()));
	}

	@Test
	void processSellClosesPositionWhenQuantityIsFullySold() {
		OrderEntity order = insertSellOrder(5);
		CurrentPriceEntity currentPrice = currentPrice("25.0000");
		PositionsEntity position = position(405, 5, "100.0000", "20.0000");

		when(ordersRepo.findByIdForUpdate(order.getOrderId())).thenReturn(Optional.of(order));
		when(accountsRepo.findByIdForUpdate(account.getAccountId())).thenReturn(Optional.of(account));
		when(currentPriceRepo.findByInstrumentId(instrument.getInstrumentId())).thenReturn(Optional.of(currentPrice));
		when(positionsRepo.findOpenForUpdate(account.getAccountId(), instrument.getInstrumentId())).thenReturn(Optional.of(position));

		orderProcessingService.process(order.getOrderId());

		verify(positionsRepo).update(
			405,
			account.getAccountId(),
			instrument.getInstrumentId(),
			0,
			BigDecimal.ZERO,
			new BigDecimal("20.0000"),
			LocalDateTime.of(2024, 9, 1, 9, 30),
			EXPECTED_HAPPENED_AT);
		verify(accountsRepo).updateBalance(account.getAccountId(), new BigDecimal("625.0000"));
		verify(ordersRepo).updateExecutionOutcome(
			order.getOrderId(),
			new BigDecimal("125.0000"),
			"FILLED",
			EXPECTED_HAPPENED_AT);
		verify(historicalOrdersRepo).insert(eq(order.getOrderId()), eq(account.getAccountId()), anyString(), eq(order.getCreatedAt()));
	}

	private OrderEntity insertSellOrder(int quantity) {
		OrderEntity order = new OrderEntity();
		order.setOrderId(101 + quantity);
		order.setSide("SELL");
		order.setAccountId(account);
		order.setInstrumentId(instrument);
		order.setStatus("PENDING");
		order.setQuantity(quantity);
		order.setTotalPrice(new BigDecimal("0.0000"));
		order.setCreatedAt(LocalDateTime.of(2026, 1, 5, 9, 55));
		return order;
	}

	private CurrentPriceEntity currentPrice(String price) {
		CurrentPriceEntity currentPrice = new CurrentPriceEntity();
		currentPrice.setInstrumentId(instrument.getInstrumentId());
		currentPrice.setPrice(new BigDecimal(price));
		return currentPrice;
	}

	private PositionsEntity position(int positionId, int quantity, String totalPrice, String averagePrice) {
		PositionsEntity position = new PositionsEntity();
		position.setQuantity(quantity);
		position.setTotalPrice(new BigDecimal(totalPrice));
		position.setAveragePrice(new BigDecimal(averagePrice));
		position.setOpenedAt(LocalDateTime.of(2024, 9, 1, 9, 30));
		position.setClosedAt(null);
		position.setAccountId(account);
		position.setInstrumentId(instrument);
		setPositionId(position, positionId);
		return position;
	}

	private void setPositionId(PositionsEntity position, int positionId) {
		try {
			Field field = PositionsEntity.class.getDeclaredField("positionId");
			field.setAccessible(true);
			field.set(position, positionId);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Unable to set test position id", ex);
		}
	}
}
