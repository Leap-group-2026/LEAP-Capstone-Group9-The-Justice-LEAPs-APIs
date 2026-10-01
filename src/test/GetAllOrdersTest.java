import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import main.Application;
import dto.response.OrderAdminResponse;
import repos.AdminRepo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class GetAllOrdersTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AdminRepo adminRepo;
    
    @Test
    void testGetAllOrdersReturnsAllOrders() throws Exception {
        // Mock the repository to return test orders
        List<OrderAdminResponse> mockOrders = Arrays.asList(
            createMockOrder(1, 100, 1, "BUY", 10, new BigDecimal("1000.00"), "FILLED"),
            createMockOrder(2, 101, 2, "SELL", 5, new BigDecimal("750.50"), "PENDING"),
            createMockOrder(3, 102, 3, "BUY", 20, new BigDecimal("3200.75"), "FILLED")
        );
        
        when(adminRepo.getAllOrders()).thenReturn(mockOrders);
        
        String body = mockMvc.perform(get("/admin/orders"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        
        JsonNode orders = objectMapper.readTree(body);
        
        // Verify it's an array
        assertTrue(orders.isArray(), "Response should be an array");
        assertTrue(orders.size() > 0, "Should return at least one order");
        
        // Verify each order has all required fields
        for (JsonNode order : orders) {
            assertFalse(order.path("order_id").isMissingNode(), "Missing order_id");
            assertFalse(order.path("account_id").isMissingNode(), "Missing account_id");
            assertFalse(order.path("instrument_id").isMissingNode(), "Missing instrument_id");
            assertFalse(order.path("side").isMissingNode(), "Missing side");
            assertFalse(order.path("quantity").isMissingNode(), "Missing quantity");
            assertFalse(order.path("total_price").isMissingNode(), "Missing total_price");
            assertFalse(order.path("status").isMissingNode(), "Missing status");
            assertFalse(order.path("created_at").isMissingNode(), "Missing created_at");
            assertFalse(order.path("updated_at").isMissingNode(), "Missing updated_at");
        }
    }
    
    private OrderAdminResponse createMockOrder(Integer orderId, Integer accountId, Integer instrumentId,
                                               String side, Integer quantity, BigDecimal totalPrice, String status) {
        OrderAdminResponse order = new OrderAdminResponse();
        order.setOrderId(orderId);
        order.setAccountId(accountId);
        order.setInstrumentId(instrumentId);
        order.setSide(side);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }
}
