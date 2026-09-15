package main.services;

import org.springframework.stereotype.Service;
import main.repos.AdminRepo;
import main.entities.adminEntity;

@Service
public class AdminService {
    private AdminRepo repo;
    public AdminService(AdminRepo repo){
        this.repo = repo;
    }

    public adminEntity saveAdmin(adminEntity entity){
        return repo.save(entity);
    }
}
