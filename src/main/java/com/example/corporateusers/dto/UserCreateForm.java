package com.example.corporateusers.dto;

import com.example.corporateusers.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserCreateForm {

    @NotBlank
    @Size(min = 3, max = 80)
    private String username;

    @NotBlank
    @Email
    @Size(max = 160)
    private String email;

    @NotBlank
    @Size(max = 160)
    private String fullName;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotNull
    private UserStatus status = UserStatus.ACTIVE;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
