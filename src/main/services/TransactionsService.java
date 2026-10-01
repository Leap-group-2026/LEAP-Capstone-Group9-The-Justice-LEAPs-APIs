package services;

import org.springframework.stereotype.Service;


import repos.TransactionsRepo;
import entities.TransactionsEntity;
import dto.response.TransactionHistoryResponse;
import java.util.List;
import services.resolver.AccountResolver;

@Service 
public class TransactionsService {
    private TransactionsRepo repo;
    private AccountResolver accountResolver;

    public TransactionsService(TransactionsRepo repo, AccountResolver accountResolver) {
        this.repo = repo;
        this.accountResolver = accountResolver;
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

    
}
