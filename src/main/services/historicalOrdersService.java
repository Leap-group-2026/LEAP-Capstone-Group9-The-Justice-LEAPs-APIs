package main.services;

import org.springframework.stereotype.Service;
import main.repos.historicalOrdersRepo;
import main.entities.historicalOrdersEntity;

@Service
public class historicalOrdersService {
    private historicalOrdersRepo repo;
    public historicalOrdersService(historicalOrdersRepo repo){
        this.repo = repo;
    }

    public historicalOrdersEntity saveHistoricalOrder(historicalOrdersEntity entity){
        return repo.save(entity);
    }
}