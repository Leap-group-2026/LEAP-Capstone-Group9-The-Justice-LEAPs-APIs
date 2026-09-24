package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.AdminEntity;
import java.util.Optional;

@Repository
public interface AdminRepo extends JpaRepository<AdminEntity, Integer>{
    boolean existsByUsername(String username);
    Optional<AdminEntity> findByUsername(String username);
}