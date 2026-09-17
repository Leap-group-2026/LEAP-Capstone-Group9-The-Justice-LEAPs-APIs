package main.services;

import org.springframework.stereotype.Service;
import main.repos.instrumentRepo;
import main.entities.instrumentEntity;

@Service
public class InstrumentService {
    private instrumentRepo repo;
    public InstrumentService(instrumentRepo repo){
        this.repo = repo;
    }

    public instrumentEntity saveInstrument(instrumentEntity entity){
        return repo.save(entity);
    }
}