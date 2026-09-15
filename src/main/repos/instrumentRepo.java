package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.instrumentEntity;

@Repository
public interface instrumentRepo extends JpaRepository<instrumentEntity, Integer>{}