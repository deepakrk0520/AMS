package com.shopsphere.api.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shopsphere.api.security.JwtService;
import com.shopsphere.api.security.Role;
import com.shopsphere.api.user.User;
import com.shopsphere.api.user.UserRepository;

/** Registration and credential login. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Creates a CUSTOMER account. Public registration can never create an ADMIN. */
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(request.firstName().trim(), request.lastName().trim(), email,
                passwordEncoder.encode(request.password()), Role.CUSTOMER);
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // Lost a race with a concurrent registration for the same email.
            throw new EmailAlreadyRegisteredException();
        }
    }

    public TokenResponse login(LoginRequest request) {
        String email = normalize(request.email());
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException ex) {
            // Same response for unknown user, bad password, and disabled account.
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        JwtService.IssuedToken token = jwtService.issue(user.getEmail(), user.getRole());
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }

    public UserResponse currentUser(String email) {
        return userRepository.findByEmail(normalize(email)).map(UserResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
