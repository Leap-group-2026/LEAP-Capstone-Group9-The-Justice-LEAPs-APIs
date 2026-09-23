package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import main.entities.AccountsEntity;
import main.entities.PortfolioSize;
import main.entities.PositionsEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionsRepo extends JpaRepository<PositionsEntity, Integer> {
    List<PositionsEntity> findByAccount(AccountsEntity accountId);


}
