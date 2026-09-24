package main.services.resolver;

import org.springframework.stereotype.Component;
import main.entities.AccountsEntity;
import main.exception.ResourceNotFoundException;
import main.repos.AccountsRepo;

@Component
public class AccountResolver {
    
    private final AccountsRepo accountsRepo;

    public AccountResolver(AccountsRepo accountsRepo) {
        this.accountsRepo = accountsRepo;
    }

    public AccountsEntity resolve(Integer accountId) {
        return accountsRepo.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account", accountId.toString()));
    }
}
