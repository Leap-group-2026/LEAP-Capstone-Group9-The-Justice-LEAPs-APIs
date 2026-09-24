package main.services;

import org.springframework.stereotype.Service;
import main.repos.InstrumentRepo;
import main.entities.InstrumentEntity;

@Service
public class InstrumentService {
    private InstrumentRepo repo;
    public InstrumentService(InstrumentRepo repo){
        this.repo = repo;
    }

    public InstrumentEntity saveInstrument(InstrumentEntity entity) {
        repo.insert(entity);
        return entity;
    }
}