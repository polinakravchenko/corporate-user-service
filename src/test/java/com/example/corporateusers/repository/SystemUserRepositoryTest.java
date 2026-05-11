package com.example.corporateusers.repository;

import com.example.corporateusers.entity.PasswordHistory;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static com.example.corporateusers.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SystemUserRepositoryTest {

    @Autowired
    private SystemUserRepository userRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Test
    void existsByUsernameIgnoreCase_shouldIgnoreCase() {
        userRepository.save(user("CaseUser", "case@example.com", "Case User", UserStatus.ACTIVE));

        assertThat(userRepository.existsByUsernameIgnoreCase("caseuser")).isTrue();
        assertThat(userRepository.existsByUsernameIgnoreCase("missing")).isFalse();
    }

    @Test
    void existsByEmailIgnoreCase_shouldIgnoreCase() {
        userRepository.save(user("email-user", "Email.User@example.com", "Email User", UserStatus.ACTIVE));

        assertThat(userRepository.existsByEmailIgnoreCase("email.user@EXAMPLE.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("other@example.com")).isFalse();
    }

    @Test
    void findByUsernameIgnoreCase_shouldReturnUser() {
        userRepository.save(user("Manager", "manager@example.com", "Manager User", UserStatus.PENDING));

        Optional<SystemUser> result = userRepository.findByUsernameIgnoreCase("manager");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("manager@example.com");
    }

    @Test
    void findByStatusOrderByCreatedAtDesc_shouldReturnOnlySelectedStatus() {
        userRepository.save(user("active", "active@example.com", "Active User", UserStatus.ACTIVE));
        userRepository.save(user("blocked", "blocked@example.com", "Blocked User", UserStatus.BLOCKED));

        assertThat(userRepository.findByStatusOrderByCreatedAtDesc(UserStatus.BLOCKED))
                .extracting(SystemUser::getUsername)
                .containsExactly("blocked");
    }

    @Test
    void search_shouldSearchByUsernameEmailAndFullNameIgnoringCase() {
        userRepository.save(user("alpha", "alpha@example.com", "Alpha Person", UserStatus.ACTIVE));
        userRepository.save(user("bravo", "bravo@corporate.example", "Bravo Person", UserStatus.PENDING));
        userRepository.save(user("charlie", "charlie@example.com", "Corporate Administrator", UserStatus.ACTIVE));

        assertThat(userRepository.search("ALP"))
                .extracting(SystemUser::getUsername)
                .containsExactly("alpha");

        assertThat(userRepository.search("corporate"))
                .extracting(SystemUser::getUsername)
                .containsExactlyInAnyOrder("bravo", "charlie");
    }

    @Test
    void findByIdWithPasswordHistory_shouldLoadPasswordHistory() {
        SystemUser user = user("history", "history@example.com", "History User", UserStatus.ACTIVE);
        user.addPasswordHistory(new PasswordHistory("another-hash", "admin"));
        SystemUser saved = userRepository.saveAndFlush(user);

        SystemUser result = userRepository.findByIdWithPasswordHistory(saved.getId()).orElseThrow();

        assertThat(result.getPasswordHistory()).hasSize(2);
    }

    @Test
    void passwordHistoryRepository_shouldReturnHistoryOrderedByChangedAtDesc() {
        SystemUser user = user("password-history", "password-history@example.com", "Password History", UserStatus.ACTIVE);
        user.addPasswordHistory(new PasswordHistory("another-hash", "admin"));
        SystemUser saved = userRepository.saveAndFlush(user);

        assertThat(passwordHistoryRepository.findByUserIdOrderByChangedAtDesc(saved.getId()))
                .hasSize(2)
                .allSatisfy(item -> assertThat(item.getUser().getId()).isEqualTo(saved.getId()));
    }
}
