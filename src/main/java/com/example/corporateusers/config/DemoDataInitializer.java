package com.example.corporateusers.config;

import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.repository.SystemUserRepository;
import com.example.corporateusers.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {

    private final SystemUserRepository userRepository;
    private final UserService userService;

    public DemoDataInitializer(SystemUserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        create("admin", "admin@example.com", "System Administrator", "AdminPass123", UserStatus.ACTIVE);
        create("manager", "manager@example.com", "Corporate Manager", "ManagerPass123", UserStatus.PENDING);
        create("blocked.user", "blocked@example.com", "Blocked User", "BlockedPass123", UserStatus.BLOCKED);
    }

    private void create(String username, String email, String fullName, String password, UserStatus status) {
        UserCreateForm form = new UserCreateForm();
        form.setUsername(username);
        form.setEmail(email);
        form.setFullName(fullName);
        form.setPassword(password);
        form.setStatus(status);
        userService.create(form);
    }
}
