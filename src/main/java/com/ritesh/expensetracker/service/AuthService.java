package com.ritesh.expensetracker.service;

import com.ritesh.expensetracker.model.Users;
import com.ritesh.expensetracker.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;

    //Register User
    public Users registerUser(Users user){
        if(user.getEmail() == null || user.getEmail().isEmpty()){
            throw new IllegalArgumentException("Bro Email Cannot Be Null");
        }
        if(!user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")){
            throw new IllegalArgumentException("This Email Format");
        }
        return usersRepository.save(user);
    }

    public List<Users> getAllUsers(){
        return usersRepository.findAll();
    }
}
