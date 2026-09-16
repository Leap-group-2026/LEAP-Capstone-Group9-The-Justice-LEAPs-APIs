package services;

import org.springframework.stereotype.Service;
import repos.PositionsRepo;
import entities.positionsEntity;
import java.util.List;

@Service
public class PositionService {
    private PositionsRepo repo;
    public PositionService(PositionsRepo repo) {
        this.repo = repo;
    }

    public positionsEntity findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public positionsEntity savePosition(positionsEntity entity) {
        return repo.save(entity);
    }

    public List<positionsEntity> findByAccountId(Integer accountId) {
        return repo.findByAccountId(accountId);
    } 
    
}
