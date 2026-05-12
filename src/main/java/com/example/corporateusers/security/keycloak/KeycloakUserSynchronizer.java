package com.example.corporateusers.security.keycloak;

import com.example.corporateusers.entity.RoleCode;
import com.example.corporateusers.entity.SystemRole;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.repository.SystemRoleRepository;
import com.example.corporateusers.repository.SystemUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KeycloakUserSynchronizer {

    private static final String KEYCLOAK_MANAGED_PASSWORD_PLACEHOLDER = "{noop}KEYCLOAK_MANAGED";

    private final SystemUserRepository userRepository;
    private final SystemRoleRepository roleRepository;
    private final KeycloakPrincipalExtractor principalExtractor;

    public KeycloakUserSynchronizer(SystemUserRepository userRepository,
                                    SystemRoleRepository roleRepository,
                                    KeycloakPrincipalExtractor principalExtractor) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.principalExtractor = principalExtractor;
    }

    @Transactional
    public SystemUser synchronize(Authentication authentication) {
        String username = principalExtractor.username(authentication);
        String email = principalExtractor.email(authentication);
        String fullName = principalExtractor.fullName(authentication);
        String keycloakSubject = principalExtractor.subject(authentication);
        Set<RoleCode> roleCodes = extractApplicationRoles(authentication);

        SystemUser user = userRepository.findWithRolesByUsernameIgnoreCase(username)
                .or(() -> userRepository.findWithRolesByEmailIgnoreCase(email))
                .orElseGet(SystemUser::new);

        if (user.getUsername() == null) {
            user.setUsername(username);
        }
        user.setEmail(email);
        user.setFullName(fullName);
        user.setStatus(UserStatus.ACTIVE);
        user.setKeycloakSubject(keycloakSubject);
        if (user.getPasswordHash() == null) {
            user.setPasswordHash(KEYCLOAK_MANAGED_PASSWORD_PLACEHOLDER);
        }

        user.getRoles().clear();
        roleCodes.forEach(roleCode -> user.addRole(getRole(roleCode)));
        if (user.getRoles().isEmpty()) {
            user.addRole(getRole(RoleCode.CUSTOMER));
        }

        return userRepository.save(user);
    }

    private Set<RoleCode> extractApplicationRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .filter(role -> role.equals("ADMIN") || role.equals("CUSTOMER"))
                .map(RoleCode::valueOf)
                .collect(Collectors.toSet());
    }

    private SystemRole getRole(RoleCode roleCode) {
        return roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleCode));
    }
}
