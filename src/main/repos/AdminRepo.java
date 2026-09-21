package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.adminEntity;
import java.util.Optional;

@Repository
public interface AdminRepo extends JpaRepository<adminEntity, Integer>{
    boolean existsByUsername(String username);
    Optional<adminEntity> findByUsername(String username);
}