package services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import repos.AccountsRepo;
import repos.TransactionsRepo;
import entities.AccountsEntity;
import entities.TransactionsEntity;
import dto.response.FundsTransferResponse;
import dto.response.TransactionHistoryResponse;
import exception.InsufficientFundsException;
import exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import services.resolver.AccountResolver;

@Service
public class TransactionsService {

    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");

    private TransactionsRepo repo;
    private AccountResolver accountResolver;
    private AccountsRepo accountsRepo;
    private Clock clock;

    public TransactionsService(TransactionsRepo repo, AccountResolver accountResolver, AccountsRepo accountsRepo, Clock clock) {
        this.repo = repo;
        this.accountResolver = accountResolver;
        this.accountsRepo = accountsRepo;
        this.clock = clock;
    }
    public TransactionsEntity saveTransaction(TransactionsEntity entity) {
        Integer accountId = entity.getAccountId() != null ? entity.getAccountId().getAccountId() : null;
        repo.insert(entity.getAmount(), entity.getSide(), accountId, entity.getTransactionType(), entity.getHappenedAt());
        return entity;
    }

    public List<TransactionHistoryResponse> getTransactionsByAccountId(Integer accountId) {
        accountResolver.resolve(accountId);
        return repo.getTransactionsByAccountId(accountId);
    }

    @Transactional
    public FundsTransferResponse deposit(Integer accountId, Integer userId, BigDecimal amount) {
        AccountsEntity account = lockOwnedActiveAccount(accountId, userId);
        BigDecimal newBalance = account.getBalance().add(amount);
        return record(account, amount, newBalance, "IN", "DEPOSIT");
    }

    @Transactional
    public FundsTransferResponse withdraw(Integer accountId, Integer userId, BigDecimal amount) {
        AccountsEntity account = lockOwnedActiveAccount(accountId, userId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(amount, account.getBalance());
        }
        BigDecimal newBalance = account.getBalance().subtract(amount);
        return record(account, amount, newBalance, "OUT", "WITHDRAWAL");
    }


    private AccountsEntity lockOwnedActiveAccount(Integer accountId, Integer userId) {
        if (userId == null) {
            throw new ResourceNotFoundException("Account", accountId.toString());
        }
        return accountsRepo.findOwnedActiveByIdForUpdate(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Account", accountId.toString()));
    }

    private FundsTransferResponse record(AccountsEntity account, BigDecimal amount, BigDecimal newBalance,
                                         String side, String transactionType) {
        accountsRepo.updateBalance(account.getAccountId(), newBalance);

        TransactionsEntity transaction = new TransactionsEntity();
        transaction.setAmount(amount);
        transaction.setSide(side);
        transaction.setAccountId(account);
        transaction.setTransactionType(transactionType);
        transaction.setHappenedAt(ZonedDateTime.now(clock).withZoneSameInstant(EASTERN_ZONE).toLocalDateTime());
        repo.insertReturningId(transaction);

        TransactionHistoryResponse saved = repo.findHistoryById(transaction.getTransactionId())
            .orElseThrow(() -> new IllegalStateException("Transaction " + transaction.getTransactionId() + " was not readable after insert"));
        return new FundsTransferResponse(saved, newBalance);
    }
}
