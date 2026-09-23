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
        // Fetch the account from database to ensure it's a managed entity
        if (entity.getAccountId() != null && entity.getAccountId().getAccountId() != null) {
            AccountsEntity managedAccount = accountsRepository.findById(entity.getAccountId().getAccountId()).orElse(null);
            if (managedAccount != null) {
                entity.setAccountId(managedAccount);
            }
        }
        
        // Fetch the instrument from database to ensure it's a managed entity
        if (entity.getInstrumentId() != null && entity.getInstrumentId().getInstrumentId() != null) {
            InstrumentEntity managedInstrument = instrumentRepository.findById(entity.getInstrumentId().getInstrumentId()).orElse(null);
            if (managedInstrument != null) {
                entity.setInstrumentId(managedInstrument);
            }
        }
        
        return repo.save(entity);
    }
}
