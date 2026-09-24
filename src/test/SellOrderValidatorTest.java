import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import main.dto.request.CreateOrderRequest;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.entities.PositionsEntity;
import main.exception.InvalidOrderException;
import main.repos.PositionsRepo;
import main.services.validation.SellOrderValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SellOrderValidatorTest {
    
    @Mock
    private PositionsRepo positionsRepo;
    
    private SellOrderValidator validator;
    private CreateOrderRequest request;
    private AccountsEntity account;
    private InstrumentEntity instrument;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new SellOrderValidator(positionsRepo);
        
        request = new CreateOrderRequest("SELL", 1, 1, 100);
        
        account = new AccountsEntity();
        account.setAccountId(1);
        
        instrument = new InstrumentEntity();
        instrument.setInstrumentId(1);
        instrument.setPrice(BigDecimal.valueOf(150.00));
    }
    
    @Test
    void testValidate_ValidSellOrder() {
        PositionsEntity position = new PositionsEntity();
        position.setQuantity(200);
        position.setClosedAt(null);
        InstrumentEntity posInstrument = new InstrumentEntity();
        posInstrument.setInstrumentId(1);
        position.setInstrumentId(posInstrument);
        
        List<PositionsEntity> positions = new ArrayList<>();
        positions.add(position);
        when(positionsRepo.findByAccount(1)).thenReturn(positions);
        
        assertDoesNotThrow(() -> validator.validate(request, account, instrument));
    }
    
    @Test
    void testValidate_NullAccount() {
        assertThrows(InvalidOrderException.class, 
            () -> validator.validate(request, null, instrument));
    }
    
    @Test
    void testValidate_InvalidQuantity() {
        CreateOrderRequest zeroQuantityRequest = new CreateOrderRequest("SELL", 1, 1, 0);
        assertThrows(InvalidOrderException.class, 
            () -> validator.validate(zeroQuantityRequest, account, instrument));
    }
    
    @Test
    void testValidate_InsufficientHoldings() {
        PositionsEntity position = new PositionsEntity();
        position.setQuantity(50);
        position.setClosedAt(null);
        InstrumentEntity posInstrument = new InstrumentEntity();
        posInstrument.setInstrumentId(1);
        position.setInstrumentId(posInstrument);
        
        List<PositionsEntity> positions = new ArrayList<>();
        positions.add(position);
        when(positionsRepo.findByAccount(1)).thenReturn(positions);
        
        assertThrows(InvalidOrderException.class, 
            () -> validator.validate(request, account, instrument));
    }
    
    @Test
    void testValidate_NoPositionForInstrument() {
        List<PositionsEntity> positions = new ArrayList<>();
        when(positionsRepo.findByAccount(1)).thenReturn(positions);
        
        assertThrows(InvalidOrderException.class, 
            () -> validator.validate(request, account, instrument));
    }
}
