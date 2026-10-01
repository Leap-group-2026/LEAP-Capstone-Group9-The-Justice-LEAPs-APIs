package services.resolver;

import org.springframework.stereotype.Component;
import dto.InstrumentWithPrice;
import exception.ResourceNotFoundException;
import repos.InstrumentRepo;

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
