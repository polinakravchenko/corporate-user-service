package com.example.corporateusers.e2e;

import com.example.corporateusers.entity.RoleCode;
import com.example.corporateusers.entity.SystemRole;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.repository.SystemRoleRepository;
import com.example.corporateusers.repository.SystemUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessControlE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemUserRepository userRepository;

    @Autowired
    private SystemRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createUser("admin-test", "admin-test@example.com", RoleCode.ADMIN);
        createUser("customer-test", "customer-test@example.com", RoleCode.CUSTOMER);
    }

    @Test
    void anonymousUser_shouldBeRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void admin_shouldAccessUserManagement() throws Exception {
        mockMvc.perform(get("/users").with(user("admin-test").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void customer_shouldAccessOnlyCabinet() throws Exception {
        mockMvc.perform(get("/cabinet").with(user("customer-test").roles("CUSTOMER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users").with(user("customer-test").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginPage_shouldRenderKeycloakEntryPoint() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    private void createUser(String username, String email, RoleCode roleCode) {
        SystemRole role = roleRepository.findByCode(roleCode).orElseThrow();
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(username);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setStatus(UserStatus.ACTIVE);
        user.addRole(role);
        userRepository.save(user);
    }
}
