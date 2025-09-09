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
    public Users registerUser(Users user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }

        // Gmail validation BEFORE touching DB
        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@gmail\\.com$")) {
            throw new IllegalArgumentException("We only accept Gmail addresses.");
        }

        // Duplicate check after format validation
        if (usersRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }

        return usersRepository.save(user);
    }

    public List<Users> getAllUsers(){
        return usersRepository.findAll();
    }

    public boolean deleteUser(Long id){
        if(usersRepository.existsById(id)){
            usersRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
