package main.services;

import org.springframework.stereotype.Service;
import main.repos.PositionsRepo;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;
import main.entities.PositionsEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import java.util.List;

@Service
public class PositionService {
    private PositionsRepo repo;
    private AccountsRepo accountsRepository;
    private InstrumentRepo instrumentRepository;
    
    public PositionService(PositionsRepo repo, AccountsRepo accountsRepository, InstrumentRepo instrumentRepository) {
        this.repo = repo;
        this.accountsRepository = accountsRepository;
        this.instrumentRepository = instrumentRepository;
    }

    public PositionsEntity findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public PositionsEntity savePosition(PositionsEntity entity) {
        // Extract IDs from nested objects
        Integer accountId = entity.getAccountId() != null ? entity.getAccountId().getAccountId() : null;
        Integer instrumentId = entity.getInstrumentId() != null ? entity.getInstrumentId().getInstrumentId() : null;
        
        if (accountId != null) {
            repo.insert(accountId, instrumentId, entity.getQuantity(), entity.getAveragePrice(), entity.getOpenedAt());
        }
        return entity;
    }
}
