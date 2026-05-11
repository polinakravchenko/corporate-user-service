package com.example.corporateusers.service;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.StatusChangeForm;
import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.PasswordHistory;
import com.example.corporateusers.entity.RoleCode;
import com.example.corporateusers.entity.SystemRole;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.exception.BusinessException;
import com.example.corporateusers.repository.PasswordHistoryRepository;
import com.example.corporateusers.repository.SystemRoleRepository;
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
    private static final String ADMIN_ACTOR = "admin";

    private final SystemUserRepository userRepository;
    private final SystemRoleRepository roleRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SystemUserRepository userRepository,
                       SystemRoleRepository roleRepository,
                       PasswordHistoryRepository passwordHistoryRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
        return userRepository.findAllWithRolesOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public SystemUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public SystemUser getByUsername(String username) {
        return userRepository.findWithRolesByUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
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
        return createCustomer(form);
    }

    @Transactional
    public SystemUser createCustomer(UserCreateForm form) {
        validateUniqueUsername(form.getUsername());
        validateUniqueEmail(form.getEmail());

        SystemUser user = new SystemUser();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
        user.setStatus(form.getStatus());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.addRole(getRequiredRole(RoleCode.CUSTOMER));
        user.addPasswordHistory(new PasswordHistory(user.getPasswordHash(), ADMIN_ACTOR));
        return userRepository.save(user);
    }

    @Transactional
    public SystemUser update(Long id, UserUpdateForm form) {
        SystemUser user = getUser(id);
        applyProfileUpdate(user, form);
        return userRepository.save(user);
    }

    @Transactional
    public SystemUser updateOwnProfile(String username, UserUpdateForm form) {
        SystemUser user = getByUsername(username);
        if (!user.hasRole(RoleCode.CUSTOMER)) {
            throw new BusinessException("Only customers can update data in cabinet");
        }
        applyProfileUpdate(user, form);
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
        SystemUser user = getUser(id);
        changePasswordInternal(user, form, ADMIN_ACTOR);
        return userRepository.save(user);
    }

    @Transactional
    public SystemUser changeOwnPassword(String username, PasswordChangeForm form) {
        SystemUser user = getByUsername(username);
        if (!user.hasRole(RoleCode.CUSTOMER)) {
            throw new BusinessException("Only customers can change password in cabinet");
        }
        changePasswordInternal(user, form, user.getUsername());
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        SystemUser user = getUser(id);
        if (user.hasRole(RoleCode.ADMIN)) {
            throw new BusinessException("Administrator account cannot be deleted from this screen");
        }
        userRepository.delete(user);
    }

    private void applyProfileUpdate(SystemUser user, UserUpdateForm form) {
        if (!user.getEmail().equalsIgnoreCase(form.getEmail())
                && userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new BusinessException("Email already exists: " + form.getEmail());
        }
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
    }

    private void changePasswordInternal(SystemUser user, PasswordChangeForm form, String actor) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Password confirmation does not match");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must differ from the current password");
        }
        String newHash = passwordEncoder.encode(form.getNewPassword());
        user.setPasswordHash(newHash);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.addPasswordHistory(new PasswordHistory(newHash, actor));
        user.setFailedLoginAttempts(0);
    }

    private SystemRole getRequiredRole(RoleCode code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + code));
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
