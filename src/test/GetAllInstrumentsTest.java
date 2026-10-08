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
import dto.InstrumentWithPrice;
import repos.InstrumentRepo;
import org.springframework.context.annotation.Import;
import test.config.TestSecurityConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = Application.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
public class GetAllInstrumentsTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private InstrumentRepo instrumentRepo;
    
    @Test
    void testGetAllInstrumentsReturnsAllInstruments() throws Exception {
        // Mock the repository to return test instruments
        List<InstrumentWithPrice> mockInstruments = Arrays.asList(
            new InstrumentWithPrice(1, "AAPL", "STOCK", "Apple Inc.", "USD", new BigDecimal("150.25"), OffsetDateTime.now()),
            new InstrumentWithPrice(2, "GOOGL", "STOCK", "Alphabet Inc.", "USD", new BigDecimal("140.50"), OffsetDateTime.now()),
            new InstrumentWithPrice(3, "MSFT", "STOCK", "Microsoft Corporation", "USD", new BigDecimal("380.75"), OffsetDateTime.now())
        );
        
        when(instrumentRepo.findAll()).thenReturn(mockInstruments);
        
        String body = mockMvc.perform(get("/instruments"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        
        JsonNode instruments = objectMapper.readTree(body);
        
        // Verify it's an array
        assertTrue(instruments.isArray(), "Response should be an array");
        assertTrue(instruments.size() > 0, "Should return at least one instrument");
        
        // Verify each instrument has required fields
        for (JsonNode instrument : instruments) {
            assertFalse(instrument.path("instrument_id").isMissingNode(), "Missing instrument_id");
            assertFalse(instrument.path("ticker").isMissingNode(), "Missing ticker");
            assertFalse(instrument.path("asset_name").isMissingNode(), "Missing asset_name");
            assertFalse(instrument.path("asset_type").isMissingNode(), "Missing asset_type");
            assertFalse(instrument.path("currency").isMissingNode(), "Missing currency");
        }
    }
}
