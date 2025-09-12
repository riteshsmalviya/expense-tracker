package com.ritesh.expensetracker.controller;


import com.ritesh.expensetracker.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ritesh.expensetracker.model.Users;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Users user){
        try{
            Users registeredUser = authService.registerUser(user);
            return ResponseEntity.ok(registeredUser);
        } catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body("Email Already Exists Bro");
        }
    }

    @GetMapping("/allUsers")
    public ResponseEntity<List<Users>> getAllUsers(){
        List<Users> user = authService.getAllUsers();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/")
    public ResponseEntity<String> randomMessage(){
        return ResponseEntity.ok("Welcome to our Auth System");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser (@PathVariable Long id){
        boolean deleted = authService.deleteUser(id);
        if(deleted){
            return ResponseEntity.ok().build();
        }
        else{
            return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body("This user does not exist bro");

        }
    }
}
