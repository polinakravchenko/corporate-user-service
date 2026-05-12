package com.example.corporateusers.security.keycloak;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class KeycloakPrincipalExtractor {

    public String username(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return firstNonBlank(
                    oidcUser.getPreferredUsername(),
                    oidcUser.getClaimAsString("preferred_username"),
                    oidcUser.getEmail(),
                    oidcUser.getName()
            );
        }
        if (principal instanceof OAuth2User oauth2User) {
            Map<String, Object> attributes = oauth2User.getAttributes();
            return firstNonBlank(
                    asString(attributes.get("preferred_username")),
                    asString(attributes.get("username")),
                    asString(attributes.get("email")),
                    oauth2User.getName()
            );
        }
        return authentication.getName();
    }

    public String email(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return firstNonBlank(oidcUser.getEmail(), username(authentication) + "@local.keycloak");
        }
        if (principal instanceof OAuth2User oauth2User) {
            return firstNonBlank(asString(oauth2User.getAttributes().get("email")), username(authentication) + "@local.keycloak");
        }
        return username(authentication) + "@local.keycloak";
    }

    public String fullName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return firstNonBlank(oidcUser.getFullName(), oidcUser.getGivenName(), username(authentication));
        }
        if (principal instanceof OAuth2User oauth2User) {
            return firstNonBlank(
                    asString(oauth2User.getAttributes().get("name")),
                    asString(oauth2User.getAttributes().get("given_name")),
                    username(authentication)
            );
        }
        return username(authentication);
    }

    public String subject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        if (principal instanceof OAuth2User oauth2User) {
            return oauth2User.getName();
        }
        return authentication.getName();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "unknown";
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
