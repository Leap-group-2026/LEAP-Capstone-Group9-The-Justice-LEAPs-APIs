package main.services;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.entities.OrderEntity;
import main.repos.HistoricalOrdersRepo;
import main.repos.OrdersRepo;
import main.entities.HistoricalOrdersEntity;

@Service
public class HistoricalOrdersService {
    private static final Logger log = LoggerFactory.getLogger(HistoricalOrdersService.class);
    private final HistoricalOrdersRepo repo;
    private final ObjectMapper objectMapper;
    private final OrdersRepo ordersRepo;

    public HistoricalOrdersService(HistoricalOrdersRepo repo, ObjectMapper objectMapper, OrdersRepo ordersRepo) {
        this.repo = repo;
        this.objectMapper = objectMapper;
        this.ordersRepo = ordersRepo;
    }

    public String serializeOrderToJson(OrderEntity entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.error("Error serializing historical order entity", e);
            return null;
        }
    }

    public HistoricalOrdersEntity saveHistoricalOrder(HistoricalOrdersEntity entity){
        return repo.save(entity);
    }
}