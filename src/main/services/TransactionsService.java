package main.services;

import org.springframework.stereotype.Service;
import main.repos.TransactionsRepo;
import main.entities.TransactionsEntity;

@Service 
public class TransactionsService {
    private TransactionsRepo repo;
    public TransactionsService(TransactionsRepo repo) {
        this.repo = repo;
    }
    public TransactionsEntity saveTransaction(TransactionsEntity entity) {
        Integer accountId = entity.getAccountId() != null ? entity.getAccountId().getAccountId() : null;
        repo.insert(entity.getAmount(), entity.getSide(), accountId, entity.getTransactionType(), entity.getHappenedAt());
        return entity;
    }
}
