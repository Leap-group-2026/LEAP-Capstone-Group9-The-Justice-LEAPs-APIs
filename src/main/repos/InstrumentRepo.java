package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.InstrumentEntity;

@Repository
public interface InstrumentRepo extends JpaRepository<InstrumentEntity, Integer> {}