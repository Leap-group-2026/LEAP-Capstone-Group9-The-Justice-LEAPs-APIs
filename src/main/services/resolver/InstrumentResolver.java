package main.services.resolver;

import org.springframework.stereotype.Component;
import main.dto.InstrumentWithPrice;
import main.exception.ResourceNotFoundException;
import main.repos.InstrumentRepo;

@Component
public class InstrumentResolver {
    
    private final InstrumentRepo instrumentRepo;

    public InstrumentResolver(InstrumentRepo instrumentRepo) {
        this.instrumentRepo = instrumentRepo;
    }

    public InstrumentWithPrice resolve(Integer instrumentId) {
        return instrumentRepo.findById(instrumentId)
            .orElseThrow(() -> new ResourceNotFoundException("Instrument", instrumentId.toString()));
    }
}
