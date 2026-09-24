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
        // Extract user ID
        Integer userId = entity.getUserId() != null ? entity.getUserId().getUserId() : null;
        String portfolioSize = entity.getPortfolioSize() != null ? entity.getPortfolioSize().getValue() : null;
        Boolean accountActive = entity.getAccountActive() != null ? entity.getAccountActive() : true;
        
        repo.insert(userId, entity.getBalance(), portfolioSize, entity.getTradeType(), entity.getCreatedAt(), accountActive);
        return entity;
    }
    public String closeAccount(Integer accountId, Integer currentUserId) {
        // Checks for an existing acocuntId
        AccountsEntity existingAccount = repo.findById(accountId)
        .orElseThrow(() -> new IllegalStateException("Not a valid user"));
        // Checking to see if the user is who they say they are and if not they will not be able to close the account
        if(!existingAccount.getUserId().getUserId().equals(currentUserId)) {
            throw new IllegalStateException("You are unauthorized to close this account, it does not belong to you");
        }
        // If the account balance is not 0, closing the account will not work
        if (existingAccount.getBalance().compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("In order to close an account your balance must be exactly $0, please sell your holdings");
        }
        // If all checks pass then make the account inactive
        existingAccount.setAccountActive(false);
        repo.update(accountId, existingAccount.getUserId().getUserId(), existingAccount.getBalance(), 
                   existingAccount.getPortfolioSize().getValue(), existingAccount.getTradeType(), false);
        return "Success";
    }
}