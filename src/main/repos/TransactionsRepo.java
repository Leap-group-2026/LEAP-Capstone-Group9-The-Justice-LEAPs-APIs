package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.TransactionsEntity;

@Repository
public interface TransactionsRepo extends JpaRepository<TransactionsEntity, Integer>{}