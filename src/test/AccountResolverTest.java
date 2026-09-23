package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import main.services.resolver.AccountResolver;
import main.repos.AccountsRepo;
import main.entities.accountsEntity;
import main.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountResolverTest {
    
    private AccountResolver resolver;
    
    @Mock
    private AccountsRepo accountsRepo;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new AccountResolver(accountsRepo);
    }
    
    @Test
    void testResolveAccountSuccess() {
        accountsEntity account = new accountsEntity();
        account.setAccountId(1);
        when(accountsRepo.findById(1)).thenReturn(Optional.of(account));

        accountsEntity result = resolver.resolve(1);

        assertNotNull(result);
        assertEquals(1, result.getAccountId());
        verify(accountsRepo).findById(1);
    }
    
    @Test
    void testResolveAccountNotFound() {
        when(accountsRepo.findById(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> resolver.resolve(1)
        );
        assertTrue(exception.getMessage().contains("Account"));
        assertTrue(exception.getMessage().contains("1"));
        verify(accountsRepo).findById(1);
    }
}
