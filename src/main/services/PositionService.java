package main.services;

import org.springframework.stereotype.Service;
import main.repos.PositionsRepo;
import main.repos.AccountsRepo;
import main.repos.instrumentRepo;
import main.entities.positionsEntity;
import main.entities.accountsEntity;
import main.entities.instrumentEntity;
import java.util.List;

@Service
public class PositionService {
    private PositionsRepo repo;
    private AccountsRepo accountsRepository;
    private instrumentRepo instrumentRepository;
    
    public PositionService(PositionsRepo repo, AccountsRepo accountsRepository, instrumentRepo instrumentRepository) {
        this.repo = repo;
        this.accountsRepository = accountsRepository;
        this.instrumentRepository = instrumentRepository;
    }

    public positionsEntity findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public positionsEntity savePosition(positionsEntity entity) {
        // Fetch the account from database to ensure it's a managed entity
        if (entity.getAccountId() != null && entity.getAccountId().getAccountId() != null) {
            accountsEntity managedAccount = accountsRepository.findById(entity.getAccountId().getAccountId()).orElse(null);
            if (managedAccount != null) {
                entity.setAccountId(managedAccount);
            }
        }
        
        // Fetch the instrument from database to ensure it's a managed entity
        if (entity.getInstrumentId() != null && entity.getInstrumentId().getInstrumentId() != null) {
            instrumentEntity managedInstrument = instrumentRepository.findById(entity.getInstrumentId().getInstrumentId()).orElse(null);
            if (managedInstrument != null) {
                entity.setInstrumentId(managedInstrument);
            }
        }
        
        return repo.save(entity);
    }
}
