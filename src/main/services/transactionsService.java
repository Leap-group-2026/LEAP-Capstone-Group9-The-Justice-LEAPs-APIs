package main.services;

import org.springframework.stereotype.Service;
import main.repos.transactionsRepo;
import main.entities.transactionsEntity;

@Service 
public class transactionsService {
    private transactionsRepo repo;
    public transactionsService(transactionsRepo repo) {
        this.repo = repo;
    }
    public transactionsEntity saveTransaction(transactionsEntity entity) {
        return repo.save(entity);
    }
}
