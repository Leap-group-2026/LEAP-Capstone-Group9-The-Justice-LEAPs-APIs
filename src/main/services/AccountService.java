package main.services;

import org.springframework.stereotype.Service;
import main.repos.AccountsRepo;
import main.repos.userRepo;
import main.entities.accountsEntity;
import main.entities.userEntity;

@Service
public class AccountService {
    private AccountsRepo repo;
    private userRepo userRepository;
    
    public AccountService(AccountsRepo repo, userRepo userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public accountsEntity findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public accountsEntity saveAccount(accountsEntity entity) {
        // Fetch the user from database to ensure it's a managed entity
        if (entity.getUserId() != null && entity.getUserId().getUserId() != null) {
            userEntity managedUser = userRepository.findById(entity.getUserId().getUserId()).orElse(null);
            if (managedUser != null) {
                entity.setUserId(managedUser);
            }
        }
        return repo.save(entity);
    }
    
}
