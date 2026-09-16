package repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import entities.adminEntity;

@Repository
public interface AdminRepo extends JpaRepository<adminEntity, Integer>{}