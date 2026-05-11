package com.example.corporateusers.service;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.repository.PasswordHistoryRepository;
import com.example.corporateusers.repository.SystemUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({UserService.class, UserServiceTest.TestPasswordEncoderConfig.class})
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserRepository userRepository;

    @Test
    void create_shouldHashPasswordAndSaveHistory() {
        UserCreateForm form = new UserCreateForm();
        form.setUsername("tester");
        form.setEmail("tester@example.com");
        form.setFullName("Test User");
        form.setPassword("Password123");
        form.setStatus(UserStatus.ACTIVE);

        SystemUser saved = userService.create(form);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Password123");
        assertThat(saved.getPasswordHistory()).hasSize(1);
    }

    @Test
    @Transactional
    void changePassword_shouldCreateNewHistoryRecord() {
        UserCreateForm form = new UserCreateForm();
        form.setUsername("change-password");
        form.setEmail("change-password@example.com");
        form.setFullName("Change Password");
        form.setPassword("Password123");
        form.setStatus(UserStatus.ACTIVE);
        SystemUser saved = userService.create(form);

        PasswordChangeForm passwordForm = new PasswordChangeForm();
        passwordForm.setNewPassword("NewPassword123");
        passwordForm.setConfirmPassword("NewPassword123");
        userService.changePassword(saved.getId(), passwordForm);

        SystemUser reloaded = userRepository.findByIdWithPasswordHistory(saved.getId()).orElseThrow();
        assertThat(reloaded.getPasswordHistory()).hasSize(2);
    }

    @Component
    static class TestPasswordEncoderConfig {
        @org.springframework.context.annotation.Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }
}
