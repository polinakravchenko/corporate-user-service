package com.example.corporateusers.service;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.StatusChangeForm;
import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.PasswordHistory;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.exception.BusinessException;
import com.example.corporateusers.repository.PasswordHistoryRepository;
import com.example.corporateusers.repository.SystemUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private static final String SYSTEM_ACTOR = "system";

    private final SystemUserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SystemUserRepository userRepository,
                       PasswordHistoryRepository passwordHistoryRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<SystemUser> findUsers(String query, UserStatus status) {
        if (status != null) {
            return userRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        if (StringUtils.hasText(query)) {
            return userRepository.search(query.trim());
        }
        return userRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SystemUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public SystemUser getUserWithPasswordHistory(Long id) {
        return userRepository.findByIdWithPasswordHistory(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PasswordHistory> getPasswordHistory(Long userId) {
        return passwordHistoryRepository.findByUserIdOrderByChangedAtDesc(userId);
    }

    @Transactional
    public SystemUser create(UserCreateForm form) {
        validateUniqueUsername(form.getUsername());
        validateUniqueEmail(form.getEmail());

        SystemUser user = new SystemUser();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
        user.setStatus(form.getStatus());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.addPasswordHistory(new PasswordHistory(user.getPasswordHash(), SYSTEM_ACTOR));
        return userRepository.save(user);
    }

    @Transactional
    public SystemUser update(Long id, UserUpdateForm form) {
        SystemUser user = getUser(id);
        if (!user.getEmail().equalsIgnoreCase(form.getEmail())
                && userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new BusinessException("Email already exists: " + form.getEmail());
        }
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
        return userRepository.save(user);
    }

    @Transactional
    public SystemUser changeStatus(Long id, StatusChangeForm form) {
        SystemUser user = getUser(id);
        if (user.getStatus() == form.getStatus()) {
            return user;
        }
        user.setStatus(form.getStatus());
        user.setLastStatusChangedAt(LocalDateTime.now());
        if (form.getStatus() == UserStatus.ACTIVE) {
            user.setFailedLoginAttempts(0);
        }
        return userRepository.save(user);
    }

    @Transactional
    public SystemUser changePassword(Long id, PasswordChangeForm form) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Password confirmation does not match");
        }
        SystemUser user = getUser(id);
        if (passwordEncoder.matches(form.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must differ from the current password");
        }
        String newHash = passwordEncoder.encode(form.getNewPassword());
        user.setPasswordHash(newHash);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.addPasswordHistory(new PasswordHistory(newHash, SYSTEM_ACTOR));
        user.setFailedLoginAttempts(0);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        SystemUser user = getUser(id);
        userRepository.delete(user);
    }

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Username already exists: " + username);
        }
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email already exists: " + email);
        }
    }
}
