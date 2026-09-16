package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.historicalOrdersEntity;

@Repository
public interface historicalOrdersRepo extends JpaRepository<historicalOrdersEntity, Integer>{}