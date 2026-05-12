package com.example.corporateusers.security.keycloak;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class KeycloakRoleMapper implements GrantedAuthoritiesMapper {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mappedAuthorities = new LinkedHashSet<>();

        for (GrantedAuthority authority : authorities) {
            mappedAuthorities.add(authority);

            if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                Map<String, Object> claims = oidcUserAuthority.getIdToken().getClaims();
                mappedAuthorities.addAll(mapClaimsToAuthorities(claims));
            }

            if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                Map<String, Object> attributes = oauth2UserAuthority.getAttributes();
                mappedAuthorities.addAll(mapClaimsToAuthorities(attributes));
            }
        }

        return mappedAuthorities;
    }

    public Set<GrantedAuthority> mapClaimsToAuthorities(Map<String, Object> claims) {
        Set<GrantedAuthority> mappedAuthorities = new LinkedHashSet<>();
        extractRealmRoles(claims).forEach(role -> mappedAuthorities.add(toSpringRole(role)));
        extractClientRoles(claims).forEach(role -> mappedAuthorities.add(toSpringRole(role)));
        return mappedAuthorities;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return List.of();
        }
        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) {
            return List.of();
        }
        return roleCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractClientRoles(Map<String, Object> claims) {
        Object resourceAccess = claims.get("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> resourceAccessMap)) {
            return List.of();
        }
        return resourceAccessMap.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(clientAccess -> clientAccess.get("roles"))
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }

    private GrantedAuthority toSpringRole(String keycloakRole) {
        String normalized = keycloakRole.trim().toUpperCase();
        if (normalized.startsWith(ROLE_PREFIX)) {
            return new SimpleGrantedAuthority(normalized);
        }
        return new SimpleGrantedAuthority(ROLE_PREFIX + normalized);
    }
}
