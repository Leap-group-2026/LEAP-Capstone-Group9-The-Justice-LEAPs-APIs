package test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import main.repos.AdminRepo;
import main.entities.adminEntity;
import main.Application;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = Application.class)
public class DatabaseConnectionTest {
    @Autowired
    private AdminRepo adminRepo;
    
    @Test
    public void testConnection() {
        assertNotNull(adminRepo);
    }

    

}