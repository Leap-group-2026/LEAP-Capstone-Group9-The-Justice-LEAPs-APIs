import main.repos.AccountsRepo;
import main.services.AccountService;
import main.entities.AccountsEntity;
import main.entities.UserEntity;
import main.repos.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;


@ExtendWith(MockitoExtension.class)
public class CloseAccountTest {
    
    @Mock
    private AccountsRepo accountsRepo;
    
    @Mock
    private UserRepo userRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    private AccountsEntity testAccount;
    private UserEntity testUser;
    
    @BeforeEach
    public void setUp() {
        // Create test user
        testUser = new UserEntity();
        testUser.setUserId(1);
        
        // Create test account with $0 balance
        testAccount = new AccountsEntity();
        testAccount.setAccountId(1);
        testAccount.setBalance(BigDecimal.ZERO);
        testAccount.setUserId(testUser);
        testAccount.setPortfolioSize(main.entities.PortfolioSize.BALANCED);
        testAccount.setTradeType("Stock");
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setAccountActive(true);
    }
    
    @Test
    public void testCloseAccountSuccessfully() {
        // Arrange
        when(accountsRepo.findById(1)).thenReturn(Optional.of(testAccount));
        
        // Act
        String result = accountService.closeAccount(1, 1);
        
        // Assert
        assertEquals("Success", result);
        verify(accountsRepo, times(1)).findById(1);
        verify(accountsRepo, times(1)).update(1, 1, BigDecimal.ZERO, "BALANCED", "Stock", false);
    }
    
    @Test
    public void testCloseAccountNotFound() {
        // Arrange
        when(accountsRepo.findById(999)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.closeAccount(999, 1);
        });
        
        assertEquals("Not a valid user", exception.getMessage());
    }
    
    @Test
    public void testCloseAccountUnauthorized() {
        // Arrange - account belongs to user 2, but user 1 is trying to close it
        UserEntity otherUser = new UserEntity();
        otherUser.setUserId(2);
        testAccount.setUserId(otherUser);
        
        when(accountsRepo.findById(1)).thenReturn(Optional.of(testAccount));
        
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.closeAccount(1, 1);  // user 1 trying to close user 2's account
        });
        
        assertEquals("You are unauthorized to close this account, it does not belong to you", 
                     exception.getMessage());
    }
    
    @Test
    public void testCloseAccountWithNonZeroBalance() {
        // Arrange - set balance to non-zero
        testAccount.setBalance(BigDecimal.valueOf(100.00));
        when(accountsRepo.findById(1)).thenReturn(Optional.of(testAccount));
        
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.closeAccount(1, 1);
        });
        
        assertEquals("In order to close an account your balance must be exactly $0, please sell your holdings", 
                     exception.getMessage());
    }
    @Test
    public void testCloseAccountWithNegativeBalance() {
        // Arrange - set negative balance (edge case)
        testAccount.setBalance(BigDecimal.valueOf(-50.00));
        when(accountsRepo.findById(1)).thenReturn(Optional.of(testAccount));
        
        // Act & Assert - Should reject because balance is not exactly $0
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.closeAccount(1, 1);
        });
        
        assertEquals("In order to close an account your balance must be exactly $0, please sell your holdings", 
                     exception.getMessage());
    }
    
    @Test
    public void testCloseAccountWithVerySmallNonZeroBalance() {
        // Arrange - set very small balance (0.01)
        testAccount.setBalance(BigDecimal.valueOf(0.01));
        when(accountsRepo.findById(1)).thenReturn(Optional.of(testAccount));
        
        // Act & Assert - Should reject even for tiny amounts
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.closeAccount(1, 1);
        });
        
        assertTrue(exception.getMessage().contains("$0"));
    }
    
    @Test
    public void testCloseAccountSameUserDifferentAccounts() {
        // Arrange - User 1 has two accounts, trying to close account 2
        AccountsEntity account2 = new AccountsEntity();
        account2.setAccountId(2);
        account2.setBalance(BigDecimal.ZERO);
        account2.setUserId(testUser);  // Also belongs to User 1
        account2.setPortfolioSize(main.entities.PortfolioSize.HIGH);
        account2.setTradeType("Options");
        account2.setCreatedAt(LocalDateTime.now());
        account2.setAccountActive(true);
        
        // Store initial state of account 1
        boolean account1ActiveBefore = testAccount.getAccountActive();
        BigDecimal account1BalanceBefore = testAccount.getBalance();
        
        when(accountsRepo.findById(2)).thenReturn(Optional.of(account2));
        
        // Act
        String result = accountService.closeAccount(2, 1);
        
        // Assert - Should succeed because User 1 owns account 2
        assertEquals("Success", result);
        verify(accountsRepo, times(1)).findById(2);
        verify(accountsRepo, times(1)).update(2, 1, BigDecimal.ZERO, "HIGH", "Options", false);
        
        // Assert - Account 1 should remain unchanged
        assertEquals(account1ActiveBefore, testAccount.getAccountActive());
        assertEquals(account1BalanceBefore, testAccount.getBalance());
        assertTrue(testAccount.getAccountActive());  // Still active
    }
    
    @Test
    public void testCloseAccountVerifyAccountMarkedInactive() {
        // Arrange
        when(accountsRepo.findById(1)).thenReturn(Optional.of(testAccount));
        
        // Act
        accountService.closeAccount(1, 1);
        
        // Assert - Verify in-memory object was updated
        assertFalse(testAccount.getAccountActive());
        // Verify update was called with account_active=false
        verify(accountsRepo, times(1)).update(1, 1, BigDecimal.ZERO, "BALANCED", "Stock", false);
    }
}
