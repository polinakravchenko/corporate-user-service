package com.example.corporateusers.e2e;

import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.repository.PasswordHistoryRepository;
import com.example.corporateusers.repository.SystemUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemUserRepository userRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void fullUserLifecycle_shouldCreateViewUpdateChangePasswordChangeStatusAndDeleteUser() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/users")
                        .param("username", "polina")
                        .param("email", "polina@example.com")
                        .param("fullName", "Polina Kravchenko")
                        .param("password", "Password123")
                        .param("status", "PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/users/*"))
                .andReturn();

        SystemUser created = userRepository.findByUsernameIgnoreCase("polina").orElseThrow();
        assertThat(createResult.getResponse().getRedirectedUrl()).isEqualTo("/users/" + created.getId());
        assertThat(created.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(created.getPasswordHash()).isNotEqualTo("Password123");
        assertThat(passwordHistoryRepository.findByUserIdOrderByChangedAtDesc(created.getId())).hasSize(1);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("polina")));

        mockMvc.perform(get("/users/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("users/details"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Polina Kravchenko")));

        mockMvc.perform(post("/users/" + created.getId() + "/edit")
                        .param("email", "polina.kravchenko@example.com")
                        .param("fullName", "Polina K."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + created.getId()));

        SystemUser updated = userRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("polina.kravchenko@example.com");
        assertThat(updated.getFullName()).isEqualTo("Polina K.");

        mockMvc.perform(post("/users/" + created.getId() + "/password")
                        .param("newPassword", "NewPassword123")
                        .param("confirmPassword", "NewPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + created.getId()));

        assertThat(passwordHistoryRepository.findByUserIdOrderByChangedAtDesc(created.getId())).hasSize(2);

        mockMvc.perform(post("/users/" + created.getId() + "/status")
                        .param("status", "ACTIVE")
                        .param("reason", "Verified by administrator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + created.getId()));

        SystemUser activated = userRepository.findById(created.getId()).orElseThrow();
        assertThat(activated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(activated.getLastStatusChangedAt()).isNotNull();

        mockMvc.perform(post("/users/" + created.getId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        assertThat(userRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void validationErrors_shouldKeepUserOnFormPageAndNotPersistInvalidData() throws Exception {
        mockMvc.perform(post("/users")
                        .param("username", "ab")
                        .param("email", "not-email")
                        .param("fullName", "")
                        .param("password", "123")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/create"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void searchAndStatusFilter_shouldReturnFilteredResults() throws Exception {
        mockMvc.perform(post("/users")
                        .param("username", "active.user")
                        .param("email", "active.user@example.com")
                        .param("fullName", "Active User")
                        .param("password", "Password123")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/users")
                        .param("username", "blocked.user")
                        .param("email", "blocked.user@example.com")
                        .param("fullName", "Blocked User")
                        .param("password", "Password123")
                        .param("status", "BLOCKED"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/users").param("query", "active"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("active.user")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("blocked.user"))));

        mockMvc.perform(get("/users").param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("blocked.user")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("active.user"))));
    }
}
