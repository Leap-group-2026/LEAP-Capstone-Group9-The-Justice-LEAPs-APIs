package services.resolver;

import org.springframework.stereotype.Component;
import entities.AccountsEntity;
import exception.ResourceNotFoundException;
import repos.AccountsRepo;

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
