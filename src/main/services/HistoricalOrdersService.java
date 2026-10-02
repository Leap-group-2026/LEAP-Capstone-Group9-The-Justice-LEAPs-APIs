package services;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import entities.OrderEntity;
import repos.HistoricalOrdersRepo;
import repos.OrdersRepo;
import entities.HistoricalOrdersEntity;
import dto.OrderSnapshot;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoricalOrdersService {
    private final HistoricalOrdersRepo repo;
    private final ObjectMapper objectMapper;
    private final OrdersRepo ordersRepo;

    public HistoricalOrdersService(HistoricalOrdersRepo repo, ObjectMapper objectMapper, OrdersRepo ordersRepo) {
        this.repo = repo;
        this.objectMapper = objectMapper;
        this.ordersRepo = ordersRepo;
    }

    public void captureOrderSnapshot(OrderEntity order) {
        captureOrderSnapshot(order, order.getCreatedAt());
    }

 
    public void captureOrderSnapshot(OrderEntity order, LocalDateTime occuredAt) {
        Integer instrumentId = order.getInstrumentId().getInstrumentId();
        Integer accountId = order.getAccountId().getAccountId();
        

        OrderSnapshot snapshot = new OrderSnapshot(
            order.getStatus(),                             
            order.getSide(),
            order.getQuantity(),
            order.getTotalPrice(),
            instrumentId,                                  
            accountId,                                     
            order.getUpdatedAt()
        );
        
        String jsonSnapshot = serializeSnapshotToJson(snapshot);
        
        repo.insert(
            order.getOrderId(),
            accountId,
            jsonSnapshot,
            occuredAt
        );
    }


    public List<HistoricalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return repo.findByOrderId_OrderIdOrderByCreatedAtAsc(orderId);
    }

  
    private String serializeSnapshotToJson(OrderSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Order snapshot serialization failed", e);
        }
    }

    public String serializeOrderToJson(OrderEntity entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            return null;
        }
    }

    public HistoricalOrdersEntity saveHistoricalOrder(HistoricalOrdersEntity entity){
        repo.insert(entity.getOrderId().getOrderId(), entity.getAccount().getAccountId(),
                    entity.getOrderInformationJson(), entity.getCreatedAt());
        return entity;
    }
}
