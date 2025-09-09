package com.ritesh.expensetracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String Name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    //Default Constructor
    public Users(){}

    //Constructor with Parameters
    public Users(Long id, String email, String Name, String password){
        this.id = id;
        this.email = email;
        this.Name = Name;
        this.password = password;
    }

    //Getters and Setters
    public Long getId(){
        return id;
    }

    public String getName(){
        return Name;
    }

    public String getEmail(){
        return email;
    }

    public String getPassword(){
        return password;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setName(String Name){
        this.Name = Name;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setPassword(String password){
        this.password = password;
    }
}
