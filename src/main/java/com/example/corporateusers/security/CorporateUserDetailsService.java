package com.example.corporateusers.security;

import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.repository.SystemUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorporateUserDetailsService implements UserDetailsService {

    private final SystemUserRepository userRepository;

    public CorporateUserDetailsService(SystemUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        SystemUser user = userRepository.findWithRolesByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> userRepository.findWithRolesByEmailIgnoreCase(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
        return new CorporateUserDetails(user);
    }
}
