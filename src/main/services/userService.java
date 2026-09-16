package services;

import org.springframework.stereotype.Service;
import repos.userRepo;
import entities.userEntity; 

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
