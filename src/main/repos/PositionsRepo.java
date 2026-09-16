package repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import entities.accountsEntity;
import entities.PortfolioSize;
import entities.positionsEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionsRepo extends JpaRepository<positionsEntity, Integer> {
    List<positionsEntity> findByAccount(accountsEntity accountId);

    @Query("SELECT p FROM positionsEntity p WHERE p.account.accountId = :accountId")
     public List<positionsEntity> findByAccountId(Integer accountId);
}
