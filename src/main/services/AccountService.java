package services;

import org.springframework.stereotype.Service;
import repos.AccountsRepo;
import entities.accountsEntity;

@Service
public class AccountService {
    private AccountsRepo repo;
    public AccountService(AccountsRepo repo) {
        this.repo = repo;
    }

    public accountsEntity findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public accountsEntity saveAccount(accountsEntity entity) {
        return repo.save(entity);
    }
    
}
