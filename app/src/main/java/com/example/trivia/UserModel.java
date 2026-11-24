package com.example.trivia;

public class UserModel {
    public String username;
    public String email;
    public String role;

    public UserModel() {}

    public UserModel(String username, String email, String role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }
}