package main.services;

import org.springframework.stereotype.Service;
import main.repos.AccountsRepo;
import main.repos.UserRepo;
import main.entities.AccountsEntity;
import main.entities.UserEntity;

@Service
public class AccountService {
    private AccountsRepo repo;
    private UserRepo userRepository;
    
    public AccountService(AccountsRepo repo, UserRepo userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public AccountsEntity findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public AccountsEntity saveAccount(AccountsEntity entity) {

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.LocalDateTime.now());
        }
        
        repo.insert(entity);
        return entity;
    }
    
}
