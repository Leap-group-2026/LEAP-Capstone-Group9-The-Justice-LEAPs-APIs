package services;

import org.springframework.stereotype.Service;
import repos.PositionsRepo;
import exception.ResourceNotFoundException;
import repos.AccountsRepo;
import repos.InstrumentRepo;
import entities.PositionsEntity;
import services.resolver.AccountResolver;
import dto.response.PositionResponse;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class PositionService {
    private PositionsRepo repo;
    private AccountsRepo accountsRepository;
    private InstrumentRepo instrumentRepository;
    private AccountResolver accountResolver;
    
    public PositionService(PositionsRepo repo, AccountsRepo accountsRepository, InstrumentRepo instrumentRepository, AccountResolver accountResolver) {
        this.repo = repo;
        this.accountsRepository = accountsRepository;
        this.instrumentRepository = instrumentRepository;
        this.accountResolver = accountResolver;
    }

    public PositionsEntity findById(Integer id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Position", String.valueOf(id)));
    }

    public PositionsEntity savePosition(PositionsEntity entity) {
        // Extract IDs from nested objects
        Integer accountId = entity.getAccountId() != null ? entity.getAccountId().getAccountId() : null;
        Integer instrumentId = entity.getInstrumentId() != null ? entity.getInstrumentId().getInstrumentId() : null;
        
        // Auto-set openedAt to now() if not provided
        LocalDateTime openedAt = entity.getOpenedAt() != null ? entity.getOpenedAt() : LocalDateTime.now();
        
        if (accountId != null) {
            repo.insert(accountId, instrumentId, entity.getQuantity(), entity.getTotalPrice(), 
                       entity.getAveragePrice(), openedAt, entity.getClosedAt());
        }
        return entity;
    }

    public List<PositionResponse> findOpenPositionsByAccountId(Integer accountId) {
        accountResolver.resolve(accountId);
        return repo.findOpenPositionsByAccountId(accountId);
    }
}
