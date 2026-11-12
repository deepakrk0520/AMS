package com.shopsphere.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
            log.warn("Registration rejected: email already registered");
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(request.firstName().trim(), request.lastName().trim(), email,
                passwordEncoder.encode(request.password()), Role.CUSTOMER);
        try {
            User saved = userRepository.saveAndFlush(user);
            log.info("User registered: id={}, role={}", saved.getId(), saved.getRole());
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Lost a race with a concurrent registration for the same email.
            log.warn("Registration rejected: duplicate email (concurrent request)");
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
            // The email is deliberately not logged (PII); the request id links this to the access log.
            log.warn("Login failed: {}", ex.getClass().getSimpleName());
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        JwtService.IssuedToken token = jwtService.issue(user.getEmail(), user.getRole());
        log.info("Login succeeded: userId={}", user.getId());
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
