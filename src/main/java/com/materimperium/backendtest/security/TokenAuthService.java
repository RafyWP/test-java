package com.materimperium.backendtest.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class TokenAuthService {

    private static final Map<String, List<String>> TOKENS = Map.of(
            "token-envio", List.of("ROLE_ENVIO"),
            "token-consulta", List.of("ROLE_CONSULTA"),
            "token-full", List.of("ROLE_ENVIO", "ROLE_CONSULTA"));

    public Optional<UsernamePasswordAuthenticationToken> authenticate(String token) {
        List<String> roles = TOKENS.get(token);
        if (roles == null) {
            return Optional.empty();
        }

        Collection<? extends GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return Optional.of(new UsernamePasswordAuthenticationToken(token, token, authorities));
    }
}
