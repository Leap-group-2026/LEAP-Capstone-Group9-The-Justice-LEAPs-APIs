package main.repos;

import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.UserEntity;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, Integer>{
    boolean existsByEmail(String email);
    boolean existsBySsnHash(String ssnHash);

    Optional<UserEntity> findByEmail(String email);
}
