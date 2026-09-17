package main.services;

import org.springframework.stereotype.Service;
import main.repos.userRepo;
import main.entities.userEntity; 

@Service
public class userService {
    private userRepo repo; 
    public userService(userRepo repo){
        this.repo = repo; 
    }

    public userEntity saveUser(userEntity entity){
        return repo.save(entity);
    }
}
