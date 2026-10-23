package com.shopsphere.api.auth;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopsphere.api.security.JwtService;
import com.shopsphere.api.security.Role;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim().toLowerCase(), request.password()));
        } catch (AuthenticationException ex) {
            // Same response for unknown user, bad password, and disabled account.
            throw new InvalidCredentialsException();
        }
        Role role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.substring("ROLE_".length()))
                .map(Role::valueOf)
                .findFirst()
                .orElseThrow(InvalidCredentialsException::new);
        JwtService.IssuedToken token = jwtService.issue(authentication.getName(), role);
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    /** Example protected endpoint: any authenticated user. */
    @GetMapping("/auth/me")
    public Map<String, Object> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return Map.of("email", authentication.getName(), "roles", roles);
    }

    /** Example admin-only endpoint, enforced in SecurityConfig. */
    @GetMapping("/admin/ping")
    public ResponseEntity<Map<String, String>> adminPing() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
