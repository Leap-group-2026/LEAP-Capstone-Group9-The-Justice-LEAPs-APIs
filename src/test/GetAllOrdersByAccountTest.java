import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import main.Application;
import dto.response.OrderAccountResponse;
import services.AccountService;
import entities.AccountsEntity;
import entities.UserEntity;
import org.springframework.context.annotation.Import;
import test.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = Application.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
@Transactional
public class GetAllOrdersByAccountTest {
    @Autowired 
    MockMvc mockMvc;
    
    @MockBean
    AccountService accountService;

    @Test
    public void getAllOrdersByAccountSuccess() throws Exception {
        // Mock the account lookup first
        AccountsEntity mockAccount = new AccountsEntity();
        mockAccount.setAccountId(10);
        UserEntity mockUser = new UserEntity();
        mockUser.setUserId(1);
        mockAccount.setUserId(mockUser);
        when(accountService.findById(10)).thenReturn(mockAccount);
        
        OrderAccountResponse order1 = new OrderAccountResponse(
            "BUY",
            "AAPL",
            10,
            new BigDecimal("1500.00")
        );
        order1.setStatus("FILLED");
        order1.setCreatedAt(LocalDateTime.now());
        order1.setUpdatedAt(LocalDateTime.now());
        
        OrderAccountResponse order2 = new OrderAccountResponse(
            "SELL",
            "GOOGL",
            5,
            new BigDecimal("750.00")
        );
        order2.setStatus("PENDING");
        order2.setCreatedAt(LocalDateTime.now());
        order2.setUpdatedAt(LocalDateTime.now());
        
        List<OrderAccountResponse> orders = Arrays.asList(order1, order2);
        when(accountService.getAllOrdersById(10)).thenReturn(orders);
        
        mockMvc.perform(get("/accounts/orders/10")
            .with(user("1").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].side").value("BUY"))
            .andExpect(jsonPath("$[0].instrument").value("AAPL"))
            .andExpect(jsonPath("$[1].side").value("SELL"))
            .andExpect(jsonPath("$[1].instrument").value("GOOGL"));
    }
    
    @Test
    public void getAllOrdersByAccountEmpty() throws Exception {
        // Mock the account lookup first
        AccountsEntity mockAccount = new AccountsEntity();
        mockAccount.setAccountId(10);
        UserEntity mockUser = new UserEntity();
        mockUser.setUserId(1);
        mockAccount.setUserId(mockUser);
        when(accountService.findById(10)).thenReturn(mockAccount);
        
        List<OrderAccountResponse> orders = Arrays.asList();
        when(accountService.getAllOrdersById(10)).thenReturn(orders);
        
        mockMvc.perform(get("/accounts/orders/10")
            .with(user("1").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
