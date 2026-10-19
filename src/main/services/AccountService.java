package services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repos.AccountsRepo;
import repos.UserRepo;
import repos.OrdersRepo;
import repos.InstrumentRepo;
import repos.PositionsRepo;
import repos.CurrentPriceRepo;
import repos.TransactionsRepo;
import entities.AccountsEntity;
import entities.OrderEntity;
import entities.InstrumentEntity;
import entities.PositionsEntity;
import dto.response.AccountResponse;
import dto.response.OrderAccountResponse;
import exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {
    private AccountsRepo repo;
    private UserRepo userRepository;
    private OrdersRepo ordersRepo;
    private InstrumentRepo instrumentRepo;
    private PositionsRepo positionsRepo;
    private CurrentPriceRepo currentPriceRepo;
    private TransactionsRepo transactionsRepo;
    private HistoricalOrdersService historicalOrdersService;
    private Clock clock;
    
    public AccountService(AccountsRepo repo, UserRepo userRepository, OrdersRepo ordersRepo, InstrumentRepo instrumentRepo,
                          PositionsRepo positionsRepo, CurrentPriceRepo currentPriceRepo, TransactionsRepo transactionsRepo,
                          HistoricalOrdersService historicalOrdersService, Clock clock) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.ordersRepo = ordersRepo;
        this.instrumentRepo = instrumentRepo;
        this.positionsRepo = positionsRepo;
        this.currentPriceRepo = currentPriceRepo;
        this.transactionsRepo = transactionsRepo;
        this.historicalOrdersService = historicalOrdersService;
        this.clock = clock;
    }

    public AccountsEntity findById(Integer id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Account", String.valueOf(id)));
    }


    public Optional<AccountsEntity> findIfPresent(Integer id) {
        return repo.findById(id);
    }

    public AccountsEntity saveAccount(AccountsEntity entity) {
        if (entity.getAccountActive() == null) {
            entity.setAccountActive(true);
        }

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }

  
        repo.insert(entity);
        return entity;
    }
    public String closeAccount(Integer accountId, Integer currentUserId) {
        return close(accountId, currentUserId, false);
    }


    @Transactional
    public String closeAccountAsAdmin(Integer accountId) {
        AccountsEntity source = repo.findByIdIncludingInactiveForUpdate(accountId)
            .orElseThrow(() -> new IllegalStateException("Not a valid user"));
        validateClosable(source);

        if (source.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("In order to close an account your balance must be exactly $0, please sell your holdings");
        }

        Integer userId = source.getUserId().getUserId();

        LocalDateTime happenedAt = ZonedDateTime.now(clock)
            .withZoneSameInstant(ZoneId.of("America/New_York"))
            .toLocalDateTime();
        BigDecimal liquidationValue = BigDecimal.ZERO;

        for (PositionsEntity position : positionsRepo.findOpenByAccountForUpdate(accountId)) {
            Integer quantity = position.getQuantity();
            Integer instrumentId = position.getInstrumentId().getInstrumentId();
            BigDecimal price = currentPriceRepo.findByInstrumentId(instrumentId)
                .orElseThrow(() -> new IllegalStateException("No current price is available for instrument " + instrumentId))
                .getPrice();
            BigDecimal proceeds = price.multiply(BigDecimal.valueOf(quantity));
            liquidationValue = liquidationValue.add(proceeds);

            OrderEntity liquidationOrder = new OrderEntity();
            liquidationOrder.setSide("SELL");
            liquidationOrder.setAccountId(source);
            InstrumentEntity instrument = new InstrumentEntity();
            instrument.setInstrumentId(instrumentId);
            liquidationOrder.setInstrumentId(instrument);
            liquidationOrder.setStatus("FILLED");
            liquidationOrder.setQuantity(quantity);
            liquidationOrder.setTotalPrice(proceeds);
            liquidationOrder.setCreatedAt(happenedAt);
            liquidationOrder.setUpdatedAt(happenedAt);
            ordersRepo.insert(liquidationOrder);
            historicalOrdersService.captureOrderSnapshot(liquidationOrder, happenedAt);

            positionsRepo.update(position.getPositionId(), accountId, instrumentId, 0,
                BigDecimal.ZERO, position.getAveragePrice(), position.getOpenedAt(), happenedAt);
        }

        BigDecimal sourceBalance = source.getBalance().add(liquidationValue);
        if (sourceBalance.compareTo(BigDecimal.ZERO) > 0) {
            transactionsRepo.insertTransfer(sourceBalance, "OUT", accountId, "WITHDRAWAL", happenedAt);
        }

        repo.update(accountId, userId, BigDecimal.ZERO, source.getPortfolioSize().getValue(),
            source.getTradeType(), false);
        return "Success";
    }

    private void validateClosable(AccountsEntity account) {
        if (account.getUserId() == null || account.getUserId().getUserId() == null) {
            throw new IllegalStateException("Account user information is missing");
        }
        if (!account.getAccountActive()) {
            throw new IllegalStateException("Account is already closed");
        }
    }

    private String close(Integer accountId, Integer currentUserId, boolean asAdmin) {
        // Use unfiltered method to allow closing already-inactive accounts
        AccountsEntity existingAccount = repo.findByIdIncludingInactive(accountId)
        .orElseThrow(() -> new IllegalStateException("Not a valid user"));
        
        // Check if account user information is missing
        if (existingAccount.getUserId() == null || existingAccount.getUserId().getUserId() == null) {
            throw new IllegalStateException("Account user information is missing");
        }
        
        // Check if account is already closed
        if (!existingAccount.getAccountActive()) {
            throw new IllegalStateException("Account is already closed");
        }
        
       if(!asAdmin && !accountValidation(existingAccount, currentUserId)){
            throw new IllegalStateException("You are not allowed to do operations on this account");
       }
        // If the account balance is not 0, closing the account will not work
        if (existingAccount.getBalance().compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("In order to close an account your balance must be exactly $0, please sell your holdings");
        }
        // If all checks pass then make the account inactive
        existingAccount.setAccountActive(false);
        repo.update(accountId, existingAccount.getUserId().getUserId(), existingAccount.getBalance(), 
                   existingAccount.getPortfolioSize().getValue(), existingAccount.getTradeType(), false);
        return "Success";
    }

    public boolean accountValidation(AccountsEntity existingAccount, Integer currentUserId){
        if(existingAccount.getUserId() == null || existingAccount.getUserId().getUserId() == null) {
            return false;
        }
        if(!existingAccount.getUserId().getUserId().equals(currentUserId)) {
            throw new IllegalStateException("You are unauthorized to close this account, it does not belong to you");
        }
        return true;
    }

    public List<AccountResponse> getAccountsByUserID (int userId)
    {
        return repo.findByUser(userId).stream()
            .map(account -> new AccountResponse(
                account.getAccountId(),
                account.getUserId().getUserId(),
                account.getBalance(),
                account.getPortfolioSize().getValue(),
                account.getTradeType(),
                account.getCreatedAt(),
                account.getAccountActive()
            ))
            .collect(Collectors.toList());
    }

    public List<OrderAccountResponse> getAllOrdersById(Integer accountId){
        return ordersRepo.findAllByAccountId(accountId).stream()
            .map(order -> {
                String instrumentName = instrumentRepo.findEntityById(order.getInstrumentId().getInstrumentId())
                    .getTicker();
                OrderAccountResponse response = new OrderAccountResponse(
                    order.getSide(),
                    instrumentName,
                    order.getQuantity(),
                    order.getTotalPrice()
                );
                response.setStatus(order.getStatus());
                response.setCreatedAt(order.getCreatedAt());
                response.setUpdatedAt(order.getUpdatedAt());
                return response;
            })
            .collect(Collectors.toList());
    }
    public List<AccountResponse> getAllAccounts() {
        return repo.findAll().stream()
            .map(account -> new AccountResponse(
                account.getAccountId(),
                account.getUserId().getUserId(),
                account.getBalance(),
                account.getPortfolioSize().getValue(),
                account.getTradeType(),
                account.getCreatedAt(),
                account.getAccountActive()
            ))
            .collect(Collectors.toList());
    }
}
