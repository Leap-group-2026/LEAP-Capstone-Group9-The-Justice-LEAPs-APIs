package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.userEntity;

@Repository
public interface userRepo extends JpaRepository<userEntity, Integer>{}
