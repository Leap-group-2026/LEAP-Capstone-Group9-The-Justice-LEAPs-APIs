package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import main.services.resolver.InstrumentResolver;
import main.repos.InstrumentRepo;
import main.dto.InstrumentWithPrice;
import main.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstrumentResolverTest {
    
    private InstrumentResolver resolver;
    
    @Mock
    private InstrumentRepo instrumentRepo;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new InstrumentResolver(instrumentRepo);
    }
    
    @Test
    void testResolveInstrumentSuccess() {
        InstrumentWithPrice instrument = new InstrumentWithPrice();
        instrument.setInstrumentId(100);
        instrument.setTicker("TEST");
        instrument.setAssetType("STOCK");
        instrument.setAssetName("Test Company");
        instrument.setCurrency("USD");
        when(instrumentRepo.findById(100)).thenReturn(Optional.of(instrument));
        
        InstrumentWithPrice result = resolver.resolve(100);
        
        assertNotNull(result);
        assertEquals(100, result.getInstrumentId());
        verify(instrumentRepo).findById(100);
    }
    
    @Test
    void testResolveInstrumentNotFound() {
        when(instrumentRepo.findById(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> resolver.resolve(1)
        );
        assertTrue(exception.getMessage().contains("Instrument"));
        assertTrue(exception.getMessage().contains("1"));
        verify(instrumentRepo).findById(1);
    }
}
