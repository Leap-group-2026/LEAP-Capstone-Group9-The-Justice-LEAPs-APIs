package main.services;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import main.repos.InstrumentRepo;
import main.entities.InstrumentEntity;
import main.dto.InstrumentWithPrice;
import java.util.Optional;

@Service
public class InstrumentService {
    private InstrumentRepo repo;
    public InstrumentService(InstrumentRepo repo){
        this.repo = repo;
    }

    public InstrumentEntity saveInstrument(InstrumentEntity entity) {
        if (repo.existsByTicker(entity.getTicker())) {
            throw new IllegalArgumentException("Ticker already exists");
        }
        try {
            repo.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Ticker already exists");
        }
        return entity;
    }

    public InstrumentWithPrice getTickerPrice(String ticker)
    {
        return repo.findByTicker(ticker).orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + ticker));
    }
}
