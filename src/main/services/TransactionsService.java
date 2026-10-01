package main.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import main.repos.TransactionsRepo;
import main.entities.TransactionsEntity;
import main.dto.response.TransactionHistoryResponse;
import java.util.List;
import main.services.resolver.AccountResolver;

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
