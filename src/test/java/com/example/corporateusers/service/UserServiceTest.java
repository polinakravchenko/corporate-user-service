package com.example.corporateusers.service;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.StatusChangeForm;
import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.exception.BusinessException;
import com.example.corporateusers.repository.PasswordHistoryRepository;
import com.example.corporateusers.repository.SystemUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.corporateusers.support.TestFixtures.createForm;
import static com.example.corporateusers.support.TestFixtures.passwordForm;
import static com.example.corporateusers.support.TestFixtures.statusForm;
import static com.example.corporateusers.support.TestFixtures.updateForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({UserService.class, UserServiceTest.TestPasswordEncoderConfig.class})
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserRepository userRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void create_shouldHashPasswordAndSaveInitialPasswordHistory() {
        UserCreateForm form = createForm("tester", "tester@example.com", "Test User", UserStatus.ACTIVE);

        SystemUser saved = userService.create(form);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("tester");
        assertThat(saved.getPasswordHash()).isNotEqualTo("Password123");
        assertThat(passwordEncoder.matches("Password123", saved.getPasswordHash())).isTrue();
        assertThat(passwordHistoryRepository.findByUserIdOrderByChangedAtDesc(saved.getId())).hasSize(1);
    }

    @Test
    void create_shouldTrimTextFields() {
        UserCreateForm form = createForm("  trimmed  ", "  trimmed@example.com  ", "  Trimmed User  ", UserStatus.PENDING);

        SystemUser saved = userService.create(form);

        assertThat(saved.getUsername()).isEqualTo("trimmed");
        assertThat(saved.getEmail()).isEqualTo("trimmed@example.com");
        assertThat(saved.getFullName()).isEqualTo("Trimmed User");
    }

    @Test
    void create_shouldRejectDuplicateUsernameIgnoringCase() {
        userService.create(createForm("duplicate", "first@example.com", "First User", UserStatus.ACTIVE));

        UserCreateForm duplicate = createForm("DUPLICATE", "second@example.com", "Second User", UserStatus.ACTIVE);

        assertThatThrownBy(() -> userService.create(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void create_shouldRejectDuplicateEmailIgnoringCase() {
        userService.create(createForm("first", "duplicate@example.com", "First User", UserStatus.ACTIVE));

        UserCreateForm duplicate = createForm("second", "DUPLICATE@example.com", "Second User", UserStatus.ACTIVE);

        assertThatThrownBy(() -> userService.create(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void findUsers_shouldReturnAllUsersSortedByCreatedAtDescWhenNoFilters() {
        SystemUser first = userService.create(createForm("first", "first@example.com", "First User", UserStatus.ACTIVE));
        SystemUser second = userService.create(createForm("second", "second@example.com", "Second User", UserStatus.PENDING));

        List<SystemUser> result = userService.findUsers(null, null);

        assertThat(result)
                .extracting(SystemUser::getUsername)
                .containsExactly(second.getUsername(), first.getUsername());
    }

    @Test
    void findUsers_shouldSearchByQuery() {
        userService.create(createForm("alpha", "alpha@example.com", "Alpha Person", UserStatus.ACTIVE));
        userService.create(createForm("beta", "beta@example.com", "Beta Person", UserStatus.PENDING));

        List<SystemUser> result = userService.findUsers("alp", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("alpha");
    }

    @Test
    void findUsers_shouldFilterByStatusWhenStatusIsProvided() {
        userService.create(createForm("active-user", "active@example.com", "Active User", UserStatus.ACTIVE));
        userService.create(createForm("blocked-user", "blocked@example.com", "Blocked User", UserStatus.BLOCKED));

        List<SystemUser> result = userService.findUsers("ignored-query", UserStatus.BLOCKED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void getUser_shouldThrowWhenUserDoesNotExist() {
        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @Transactional
    void getUserWithPasswordHistory_shouldLoadPasswordHistory() {
        SystemUser saved = userService.create(createForm("history-user", "history@example.com", "History User", UserStatus.ACTIVE));

        SystemUser result = userService.getUserWithPasswordHistory(saved.getId());

        assertThat(result.getPasswordHistory()).hasSize(1);
    }

    @Test
    void update_shouldChangeEmailAndFullName() {
        SystemUser saved = userService.create(createForm("updatable", "old@example.com", "Old Name", UserStatus.ACTIVE));
        UserUpdateForm form = updateForm("new@example.com", "New Name");

        SystemUser updated = userService.update(saved.getId(), form);

        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getFullName()).isEqualTo("New Name");
    }

    @Test
    void update_shouldAllowSameEmailWithDifferentCaseForSameUser() {
        SystemUser saved = userService.create(createForm("same-email", "same@example.com", "Same User", UserStatus.ACTIVE));
        UserUpdateForm form = updateForm("SAME@example.com", "Same User Updated");

        SystemUser updated = userService.update(saved.getId(), form);

        assertThat(updated.getEmail()).isEqualTo("SAME@example.com");
        assertThat(updated.getFullName()).isEqualTo("Same User Updated");
    }

    @Test
    void update_shouldRejectEmailUsedByAnotherUser() {
        userService.create(createForm("owner", "owner@example.com", "Owner User", UserStatus.ACTIVE));
        SystemUser edited = userService.create(createForm("edited", "edited@example.com", "Edited User", UserStatus.PENDING));

        UserUpdateForm form = updateForm("OWNER@example.com", "Edited User");

        assertThatThrownBy(() -> userService.update(edited.getId(), form))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void changeStatus_shouldUpdateStatusAndTimestamp() {
        SystemUser saved = userService.create(createForm("status-user", "status@example.com", "Status User", UserStatus.PENDING));
        StatusChangeForm form = statusForm(UserStatus.BLOCKED, "Risk detected");

        SystemUser updated = userService.changeStatus(saved.getId(), form);

        assertThat(updated.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(updated.getLastStatusChangedAt()).isNotNull();
    }

    @Test
    void changeStatus_shouldReturnUserWithoutTimestampChangeWhenStatusIsSame() {
        SystemUser saved = userService.create(createForm("same-status", "same-status@example.com", "Same Status", UserStatus.ACTIVE));

        SystemUser updated = userService.changeStatus(saved.getId(), statusForm(UserStatus.ACTIVE, "No-op"));

        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updated.getLastStatusChangedAt()).isNull();
    }

    @Test
    void changeStatus_shouldResetFailedLoginAttemptsWhenUserBecomesActive() {
        SystemUser saved = userService.create(createForm("reactivate", "reactivate@example.com", "Reactivate User", UserStatus.BLOCKED));
        saved.setFailedLoginAttempts(5);
        userRepository.saveAndFlush(saved);

        SystemUser updated = userService.changeStatus(saved.getId(), statusForm(UserStatus.ACTIVE, "Verified"));

        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updated.getFailedLoginAttempts()).isZero();
    }

    @Test
    void changePassword_shouldRejectMismatchedConfirmation() {
        SystemUser saved = userService.create(createForm("password-mismatch", "password-mismatch@example.com", "Password User", UserStatus.ACTIVE));
        PasswordChangeForm form = passwordForm("NewPassword123", "AnotherPassword123");

        assertThatThrownBy(() -> userService.changePassword(saved.getId(), form))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password confirmation does not match");
    }

    @Test
    void changePassword_shouldRejectSamePassword() {
        SystemUser saved = userService.create(createForm("same-password", "same-password@example.com", "Same Password", UserStatus.ACTIVE));
        PasswordChangeForm form = passwordForm("Password123", "Password123");

        assertThatThrownBy(() -> userService.changePassword(saved.getId(), form))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("New password must differ");
    }

    @Test
    @Transactional
    void changePassword_shouldCreateNewHistoryRecordAndResetFailedLoginAttempts() {
        SystemUser saved = userService.create(createForm("change-password", "change-password@example.com", "Change Password", UserStatus.ACTIVE));
        saved.setFailedLoginAttempts(3);
        userRepository.saveAndFlush(saved);

        userService.changePassword(saved.getId(), passwordForm("NewPassword123", "NewPassword123"));

        SystemUser reloaded = userRepository.findByIdWithPasswordHistory(saved.getId()).orElseThrow();
        assertThat(reloaded.getPasswordHistory()).hasSize(2);
        assertThat(reloaded.getFailedLoginAttempts()).isZero();
        assertThat(passwordEncoder.matches("NewPassword123", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void delete_shouldRemoveUserAndPasswordHistoryByCascade() {
        SystemUser saved = userService.create(createForm("delete-me", "delete-me@example.com", "Delete User", UserStatus.DISABLED));

        userService.delete(saved.getId());

        assertThat(userRepository.findById(saved.getId())).isEmpty();
        assertThat(passwordHistoryRepository.findByUserIdOrderByChangedAtDesc(saved.getId())).isEmpty();
    }

    static class TestPasswordEncoderConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }
}
