package main.repos;

import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.userEntity;
import java.util.Optional;

@Repository
public interface userRepo extends JpaRepository<userEntity, Integer>{
    boolean existsByEmail(String email);
    boolean existsBySsnHash(String ssnHash);

    Optional<userEntity> findByEmail(String email);
}
