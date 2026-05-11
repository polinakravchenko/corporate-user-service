package com.example.corporateusers.controller;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.StatusChangeForm;
import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.PasswordHistory;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.exception.BusinessException;
import com.example.corporateusers.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void list_shouldRenderListViewWithUsersAndFilters() throws Exception {
        SystemUser user = persistedUser(1L, "admin", "admin@example.com", "System Admin", UserStatus.ACTIVE);
        when(userService.findUsers("adm", UserStatus.ACTIVE)).thenReturn(List.of(user));

        mockMvc.perform(get("/users")
                        .param("query", "adm")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users", "statuses", "selectedStatus", "query"));

        verify(userService).findUsers("adm", UserStatus.ACTIVE);
    }

    @Test
    void createForm_shouldRenderCreateView() throws Exception {
        mockMvc.perform(get("/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/create"))
                .andExpect(model().attributeExists("form", "statuses"));
    }

    @Test
    void create_shouldRedirectToDetailsWhenFormIsValid() throws Exception {
        SystemUser saved = persistedUser(15L, "new.user", "new.user@example.com", "New User", UserStatus.ACTIVE);
        when(userService.create(any(UserCreateForm.class))).thenReturn(saved);

        mockMvc.perform(post("/users")
                        .param("username", "new.user")
                        .param("email", "new.user@example.com")
                        .param("fullName", "New User")
                        .param("password", "Password123")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/15"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void create_shouldReturnCreateViewWhenValidationFails() throws Exception {
        mockMvc.perform(post("/users")
                        .param("username", "ab")
                        .param("email", "invalid-email")
                        .param("fullName", "")
                        .param("password", "short")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/create"))
                .andExpect(model().attributeHasFieldErrors("form", "username", "email", "fullName", "password"))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    void create_shouldReturnCreateViewWhenBusinessExceptionOccurs() throws Exception {
        when(userService.create(any(UserCreateForm.class)))
                .thenThrow(new BusinessException("Username already exists: admin"));

        mockMvc.perform(post("/users")
                        .param("username", "admin")
                        .param("email", "admin@example.com")
                        .param("fullName", "Admin User")
                        .param("password", "Password123")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/create"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    void details_shouldRenderDetailsView() throws Exception {
        SystemUser user = persistedUser(2L, "manager", "manager@example.com", "Manager User", UserStatus.PENDING);
        when(userService.getUserWithPasswordHistory(2L)).thenReturn(user);
        when(userService.getPasswordHistory(2L)).thenReturn(List.of(historyItem(10L, "hash", "system")));

        mockMvc.perform(get("/users/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/details"))
                .andExpect(model().attributeExists("user", "passwordHistory"));
    }

    @Test
    void editForm_shouldRenderEditViewWithPrefilledForm() throws Exception {
        when(userService.getUser(3L))
                .thenReturn(persistedUser(3L, "editor", "editor@example.com", "Editor User", UserStatus.ACTIVE));

        mockMvc.perform(get("/users/3/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/edit"))
                .andExpect(model().attributeExists("user", "form"));
    }

    @Test
    void update_shouldRedirectToDetailsWhenFormIsValid() throws Exception {
        when(userService.getUser(3L))
                .thenReturn(persistedUser(3L, "editor", "old@example.com", "Old Name", UserStatus.ACTIVE));

        mockMvc.perform(post("/users/3/edit")
                        .param("email", "new@example.com")
                        .param("fullName", "New Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/3"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).update(eq(3L), any(UserUpdateForm.class));
    }

    @Test
    void update_shouldReturnEditViewWhenValidationFails() throws Exception {
        when(userService.getUser(3L))
                .thenReturn(persistedUser(3L, "editor", "old@example.com", "Old Name", UserStatus.ACTIVE));

        mockMvc.perform(post("/users/3/edit")
                        .param("email", "wrong-email")
                        .param("fullName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("users/edit"))
                .andExpect(model().attributeHasFieldErrors("form", "email", "fullName"));
    }

    @Test
    void update_shouldReturnEditViewWhenBusinessExceptionOccurs() throws Exception {
        when(userService.getUser(3L))
                .thenReturn(persistedUser(3L, "editor", "old@example.com", "Old Name", UserStatus.ACTIVE));
        when(userService.update(eq(3L), any(UserUpdateForm.class)))
                .thenThrow(new BusinessException("Email already exists: new@example.com"));

        mockMvc.perform(post("/users/3/edit")
                        .param("email", "new@example.com")
                        .param("fullName", "New Name"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/edit"))
                .andExpect(model().hasErrors());
    }

    @Test
    void passwordForm_shouldRenderPasswordView() throws Exception {
        when(userService.getUser(4L))
                .thenReturn(persistedUser(4L, "password-user", "password@example.com", "Password User", UserStatus.ACTIVE));

        mockMvc.perform(get("/users/4/password"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/password"))
                .andExpect(model().attributeExists("user", "form"));
    }

    @Test
    void changePassword_shouldRedirectToDetailsWhenFormIsValid() throws Exception {
        mockMvc.perform(post("/users/4/password")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "NewPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/4"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).changePassword(eq(4L), any(PasswordChangeForm.class));
    }

    @Test
    void changePassword_shouldReturnPasswordViewWhenValidationFails() throws Exception {
        when(userService.getUser(4L))
                .thenReturn(persistedUser(4L, "password-user", "password@example.com", "Password User", UserStatus.ACTIVE));

        mockMvc.perform(post("/users/4/password")
                        .param("newPassword", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/password"))
                .andExpect(model().attributeHasFieldErrors("form", "newPassword", "confirmPassword"));
    }

    @Test
    void changePassword_shouldReturnPasswordViewWhenBusinessExceptionOccurs() throws Exception {
        when(userService.getUser(4L))
                .thenReturn(persistedUser(4L, "password-user", "password@example.com", "Password User", UserStatus.ACTIVE));
        when(userService.changePassword(eq(4L), any(PasswordChangeForm.class)))
                .thenThrow(new BusinessException("Password confirmation does not match"));

        mockMvc.perform(post("/users/4/password")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "OtherPassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/password"))
                .andExpect(model().hasErrors());
    }

    @Test
    void statusForm_shouldRenderStatusView() throws Exception {
        when(userService.getUser(5L))
                .thenReturn(persistedUser(5L, "status-user", "status@example.com", "Status User", UserStatus.PENDING));

        mockMvc.perform(get("/users/5/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/status"))
                .andExpect(model().attributeExists("user", "form", "statuses"));
    }

    @Test
    void changeStatus_shouldRedirectToDetailsWhenFormIsValid() throws Exception {
        mockMvc.perform(post("/users/5/status")
                        .param("status", "ACTIVE")
                        .param("reason", "Verification completed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/5"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).changeStatus(eq(5L), any(StatusChangeForm.class));
    }

    @Test
    void changeStatus_shouldReturnStatusViewWhenValidationFails() throws Exception {
        when(userService.getUser(5L))
                .thenReturn(persistedUser(5L, "status-user", "status@example.com", "Status User", UserStatus.PENDING));

        mockMvc.perform(post("/users/5/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/status"))
                .andExpect(model().attributeHasFieldErrors("form", "status"))
                .andExpect(model().attributeExists("user", "statuses"));
    }

    @Test
    void delete_shouldRedirectToUsersList() throws Exception {
        mockMvc.perform(post("/users/6/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).delete(6L);
    }

    private static PasswordHistory historyItem(Long id, String hash, String changedBy) {
        PasswordHistory history = new PasswordHistory(hash, changedBy);
        ReflectionTestUtils.setField(history, "id", id);
        ReflectionTestUtils.setField(history, "changedAt", LocalDateTime.of(2026, 5, 11, 10, 5));
        return history;
    }

    private static SystemUser persistedUser(Long id,
                                            String username,
                                            String email,
                                            String fullName,
                                            UserStatus status) {
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash("$2a$04$abcdefghijklmnopqrstuuYkSRnNpYfXwQp4g7xR7A5rBkyA0DP2O");
        user.setStatus(status);
        user.addPasswordHistory(new PasswordHistory(user.getPasswordHash(), "test"));
        LocalDateTime now = LocalDateTime.of(2026, 5, 11, 10, 0);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", now);
        ReflectionTestUtils.setField(user, "updatedAt", now);
        ReflectionTestUtils.setField(user, "passwordChangedAt", now);
        return user;
    }
}
