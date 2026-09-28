package main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import main.repos.OrdersRepo;
import main.entities.OrderEntity;
import java.util.List;

@Service
public class ScheduledOrdersCheck {

    @Autowired 
    private OrdersRepo repo;

    @Scheduled(fixedRate = 600000)
    public void checkOrders(){
        List<OrderEntity> allOrders = repo.findAll();
        for(int i = 0; i < allOrders.size(); i++){
            if(allOrders.get(i).getStatus())
        }
    }
}
