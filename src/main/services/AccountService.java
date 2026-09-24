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
        // Fetch the user from database to ensure it's a managed entity
        if (entity.getUserId() != null && entity.getUserId().getUserId() != null) {
            UserEntity managedUser = userRepository.findById(entity.getUserId().getUserId()).orElse(null);
            if (managedUser != null) {
                entity.setUserId(managedUser);
            }
        }
        return repo.save(entity);
    }
    
}
