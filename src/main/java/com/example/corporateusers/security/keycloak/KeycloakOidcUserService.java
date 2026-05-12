package com.example.corporateusers.security.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class KeycloakOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String PREFERRED_USERNAME = "preferred_username";

    private final OidcUserService delegate = new OidcUserService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KeycloakRoleMapper roleMapper;

    public KeycloakOidcUserService(KeycloakRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
        authorities.addAll(roleMapper.mapClaimsToAuthorities(oidcUser.getClaims()));
        authorities.addAll(roleMapper.mapClaimsToAuthorities(decodeJwtClaims(userRequest.getAccessToken().getTokenValue())));

        if (oidcUser.getUserInfo() != null) {
            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), PREFERRED_USERNAME);
        }
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), PREFERRED_USERNAME);
    }

    private Map<String, Object> decodeJwtClaims(String tokenValue) {
        try {
            String[] parts = tokenValue.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(decodedPayload, StandardCharsets.UTF_8);
            return objectMapper.readValue(payloadJson, new TypeReference<>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
