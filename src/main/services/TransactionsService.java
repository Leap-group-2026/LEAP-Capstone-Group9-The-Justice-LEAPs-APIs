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
        return repo.save(entity);
    }
}
