package main.services;

import org.springframework.stereotype.Service;
import main.repos.HistoricalOrdersRepo;
import main.entities.HistoricalOrdersEntity;

@Service
public class HistoricalOrdersService {
    private HistoricalOrdersRepo repo;
    public HistoricalOrdersService(HistoricalOrdersRepo repo){
        this.repo = repo;
    }

    public HistoricalOrdersEntity saveHistoricalOrder(HistoricalOrdersEntity entity){
        return repo.save(entity);
    }
}