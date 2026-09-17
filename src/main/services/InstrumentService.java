package main.services;

import org.springframework.stereotype.Service;
import main.repos.InstrumentRepo;
import main.entities.instrumentEntity;

@Service
public class InstrumentService {
    private InstrumentRepo repo;
    public InstrumentService(InstrumentRepo repo){
        this.repo = repo;
    }

    public instrumentEntity saveInstrument(instrumentEntity entity){
        return repo.save(entity);
    }
}