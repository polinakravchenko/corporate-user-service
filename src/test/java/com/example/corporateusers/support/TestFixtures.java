package com.example.corporateusers.support;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.StatusChangeForm;
import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.PasswordHistory;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static UserCreateForm createForm(String username, String email, String fullName, UserStatus status) {
        UserCreateForm form = new UserCreateForm();
        form.setUsername(username);
        form.setEmail(email);
        form.setFullName(fullName);
        form.setPassword("Password123");
        form.setStatus(status);
        return form;
    }

    public static UserUpdateForm updateForm(String email, String fullName) {
        UserUpdateForm form = new UserUpdateForm();
        form.setEmail(email);
        form.setFullName(fullName);
        return form;
    }

    public static PasswordChangeForm passwordForm(String newPassword, String confirmPassword) {
        PasswordChangeForm form = new PasswordChangeForm();
        form.setNewPassword(newPassword);
        form.setConfirmPassword(confirmPassword);
        return form;
    }

    public static StatusChangeForm statusForm(UserStatus status, String reason) {
        StatusChangeForm form = new StatusChangeForm();
        form.setStatus(status);
        form.setReason(reason);
        return form;
    }

    public static SystemUser user(String username, String email, String fullName, UserStatus status) {
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash("$2a$04$abcdefghijklmnopqrstuuYkSRnNpYfXwQp4g7xR7A5rBkyA0DP2O");
        user.setStatus(status);
        user.addPasswordHistory(new PasswordHistory(user.getPasswordHash(), "test"));
        return user;
    }
}
