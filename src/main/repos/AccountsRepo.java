package repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import entities.*;
import java.util.List;
import java.util.Optional;

@Repository 
public interface AccountsRepo extends JpaRepository<accountsEntity, Integer> {
    List<accountsEntity> findByUser(userEntity user);

}



