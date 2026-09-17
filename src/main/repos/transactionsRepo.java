package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.transactionsEntity;

@Repository
public interface transactionsRepo extends JpaRepository<transactionsEntity, Integer>{}