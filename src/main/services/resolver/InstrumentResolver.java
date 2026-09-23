package main.services.resolver;

import org.springframework.stereotype.Component;
import main.entities.instrumentEntity;
import main.exception.ResourceNotFoundException;
import main.repos.instrumentRepo;

@Component
public class InstrumentResolver {
    
    private final instrumentRepo instrumentRepo;

    public InstrumentResolver(instrumentRepo instrumentRepo) {
        this.instrumentRepo = instrumentRepo;
    }

    public instrumentEntity resolve(Integer instrumentId) {
        return instrumentRepo.findById(instrumentId)
            .orElseThrow(() -> new ResourceNotFoundException("Instrument", instrumentId.toString()));
    }
}
