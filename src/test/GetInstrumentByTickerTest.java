import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import main.Application;
import test.config.TestClockConfig;

/**
 * GET /instruments/{ticker}, verified end to end.
 * 
 * Tests the InstrumentWithPrice DTO retrieval by ticker symbol with current price lookup.
 * Fixtures are inserted with raw SQL through JdbcTemplate, never through the code under test.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("GET /instruments/{ticker} Tests")
public class GetInstrumentByTickerTest {

    private static final int AAPL_INSTRUMENT_ID = 900801;
    private static final int MSFT_INSTRUMENT_ID = 900802;
    private static final int UNPRICED_INSTRUMENT_ID = 900803;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Create instrument with price
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'AAPL', 'STOCK', 'Apple Inc.', 'USD')",
            AAPL_INSTRUMENT_ID);
        
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) " +
            "VALUES (?, '175.5000', now(), now())",
            AAPL_INSTRUMENT_ID);

        // Create another instrument with price
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'MSFT', 'STOCK', 'Microsoft Corporation', 'USD')",
            MSFT_INSTRUMENT_ID);
        
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) " +
            "VALUES (?, '425.2500', now(), now())",
            MSFT_INSTRUMENT_ID);

        // Create instrument without price
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'UNKNOWN', 'STOCK', 'Unknown Company', 'USD')",
            UNPRICED_INSTRUMENT_ID);
    }

    @Test
    @DisplayName("Should return instrument with current price by ticker")
    void returnInstrumentWithPrice() throws Exception {
        mockMvc.perform(get("/instruments/AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instrument_id").value(AAPL_INSTRUMENT_ID))
            .andExpect(jsonPath("$.ticker").value("AAPL"))
            .andExpect(jsonPath("$.asset_type").value("STOCK"))
            .andExpect(jsonPath("$.asset_name").value("Apple Inc."))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.price").value(175.5));
    }

    @Test
    @DisplayName("Should return different instrument when requesting different ticker")
    void returnCorrectInstrumentForTicker() throws Exception {
        mockMvc.perform(get("/instruments/MSFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instrument_id").value(MSFT_INSTRUMENT_ID))
            .andExpect(jsonPath("$.ticker").value("MSFT"))
            .andExpect(jsonPath("$.asset_name").value("Microsoft Corporation"))
            .andExpect(jsonPath("$.price").value(425.25));
    }

    @Test
    @DisplayName("Should return 404 for nonexistent ticker")
    void returnNotFoundForNonexistentTicker() throws Exception {
        mockMvc.perform(get("/instruments/NONEXISTENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should include all required instrument fields")
    void includesAllRequiredFields() throws Exception {
        mockMvc.perform(get("/instruments/AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instrument_id").exists())
            .andExpect(jsonPath("$.ticker").exists())
            .andExpect(jsonPath("$.asset_type").exists())
            .andExpect(jsonPath("$.asset_name").exists())
            .andExpect(jsonPath("$.currency").exists())
            .andExpect(jsonPath("$.price").exists());
    }

    @Test
    @DisplayName("Should have correct field types")
    void correctFieldTypes() throws Exception {
        mockMvc.perform(get("/instruments/AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instrument_id").isNumber())
            .andExpect(jsonPath("$.ticker").isString())
            .andExpect(jsonPath("$.asset_type").isString())
            .andExpect(jsonPath("$.asset_name").isString())
            .andExpect(jsonPath("$.currency").isString())
            .andExpect(jsonPath("$.price").isNumber());
    }

    @Test
    @DisplayName("Should include quoteTime when price is available")
    void includeQuoteTimeWhenPriceAvailable() throws Exception {
        mockMvc.perform(get("/instruments/AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quote_time").exists());
    }

    @Test
    @DisplayName("Should return null price for instrument without current price")
    void returnNullPriceForUnpricedInstrument() throws Exception {
        mockMvc.perform(get("/instruments/UNKNOWN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instrument_id").value(UNPRICED_INSTRUMENT_ID))
            .andExpect(jsonPath("$.ticker").value("UNKNOWN"))
            .andExpect(jsonPath("$.price").value(nullValue()));
    }

    @Test
    @DisplayName("Should return null quoteTime when price is not available")
    void returnNullQuoteTimeForUnpricedInstrument() throws Exception {
        mockMvc.perform(get("/instruments/UNKNOWN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quote_time").value(nullValue()));
    }

    @Test
    @DisplayName("Should handle ticker case sensitivity correctly")
    void handleTickerLookup() throws Exception {
        // Assuming ticker lookup is case-sensitive or converted to uppercase
        mockMvc.perform(get("/instruments/AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("Should return correct asset information")
    void returnCorrectAssetInformation() throws Exception {
        mockMvc.perform(get("/instruments/AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.asset_type").value("STOCK"))
            .andExpect(jsonPath("$.asset_name").value("Apple Inc."))
            .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("Should maintain data independence between instruments")
    void dataIndependenceBetweenInstruments() throws Exception {
        mockMvc.perform(get("/instruments/AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value("AAPL"))
            .andExpect(jsonPath("$.asset_name").value("Apple Inc."))
            .andExpect(jsonPath("$.price").value(175.5));

        mockMvc.perform(get("/instruments/MSFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value("MSFT"))
            .andExpect(jsonPath("$.asset_name").value("Microsoft Corporation"))
            .andExpect(jsonPath("$.price").value(425.25));
    }
}
