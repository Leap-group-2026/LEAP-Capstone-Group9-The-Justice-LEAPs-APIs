package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.adminEntity;

@Repository
public interface AdminRepo extends JpaRepository<adminEntity, Integer>{}