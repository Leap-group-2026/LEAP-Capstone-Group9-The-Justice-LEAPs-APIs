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
        Integer orderId = entity.getOrderId() != null ? entity.getOrderId().getOrderId() : null;
        Integer accountId = entity.getAccount() != null ? entity.getAccount().getAccountId() : null;
        repo.insert(orderId, accountId, entity.getOrderInformationJson(), entity.getCreatedAt());
        return entity;
    }
}