package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.*;
import java.util.List;
import java.util.Optional;

@Repository 
public interface AccountsRepo extends JpaRepository<AccountsEntity, Integer> {
    List<AccountsEntity> findByUser(UserEntity user);

}



