package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import main.entities.accountsEntity;
import main.entities.PortfolioSize;
import main.entities.positionsEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionsRepo extends JpaRepository<positionsEntity, Integer> {
    List<positionsEntity> findByAccount(accountsEntity accountId);


}
