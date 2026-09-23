package main.services;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.entities.OrderEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.repos.historicalOrdersRepo;
import main.repos.OrdersRepo;
import main.entities.historicalOrdersEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class historicalOrdersService {
    private final historicalOrdersRepo repo;
    private final ObjectMapper objectMapper;
    private final OrdersRepo ordersRepo;

    public String serializeOrderToJson(OrderEntity entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.error("Error serializing historical order entity", e);
            return null;
        }
    }

    public historicalOrdersEntity saveHistoricalOrder(historicalOrdersEntity entity){
        return repo.save(entity);
    }
}