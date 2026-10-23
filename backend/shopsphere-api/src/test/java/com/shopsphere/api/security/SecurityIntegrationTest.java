package com.shopsphere.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.shopsphere.api.auth.AuthController;
import com.shopsphere.api.controller.HealthController;
import com.shopsphere.api.exception.GlobalExceptionHandler;
import com.shopsphere.api.user.DatabaseUserDetailsService;
import com.shopsphere.api.user.User;
import com.shopsphere.api.user.UserRepository;

@WebMvcTest({ HealthController.class, AuthController.class })
@Import({ SecurityConfig.class, AuthenticationManagerConfig.class, SecurityErrorHandlers.class, JwtService.class, DatabaseUserDetailsService.class,
        GlobalExceptionHandler.class })
@TestPropertySource(properties = {
        "app.security.jwt.secret=" + SecurityIntegrationTest.SECRET,
        "app.security.jwt.issuer=test-issuer",
        "app.security.jwt.expiration=5m" })
class SecurityIntegrationTest {

    static final String SECRET = "test-only-secret-test-only-secret-0123456789";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private UserRepository userRepository;

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue("user@example.com", role).value();
    }

    private String forgedToken(String secret, String issuer, Instant issuedAt, Instant expiresAt) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject("user@example.com")
                .issuedAt(issuedAt).expiresAt(expiresAt).claim("roles", List.of("ADMIN")).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
    }

    @Test
    void validTokenGrantsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"));
    }

    @Test
    void malformedTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void tokenSignedWithWrongSecretIs401() throws Exception {
        String token = forgedToken("another-secret-another-secret-0123456789", "test-issuer", Instant.now(),
                Instant.now().plus(Duration.ofMinutes(5)));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        String token = forgedToken(SECRET, "test-issuer", Instant.now().minus(Duration.ofHours(2)),
                Instant.now().minus(Duration.ofHours(1)));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerIs401() throws Exception {
        String token = forgedToken(SECRET, "someone-else", Instant.now(), Instant.now().plus(Duration.ofMinutes(5)));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotReachAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ping").header("Authorization", bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied"));
    }

    @Test
    void adminCanReachAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ping").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithValidCredentialsReturnsUsableToken() throws Exception {
        String hash = passwordEncoder.encode("s3cret-pass");
        given(userRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(new User("User@Example.com", hash, Role.CUSTOMER)));

        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"User@Example.com\",\"password\":\"s3cret-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"));
    }

    @Test
    void loginFailuresAreIndistinguishable() throws Exception {
        given(userRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(new User("user@example.com", passwordEncoder.encode("right"), Role.CUSTOMER)));
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        String wrongPassword = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andReturn().getResponse().getContentAsString();
        String unknownUser = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid credentials")))
                .andReturn().getResponse().getContentAsString();
        assertThat(wrongPassword).doesNotContain("wrong\"");
        assertThat(unknownUser).doesNotContain("nobody");
    }

    @Test
    void loginRejectsBlankInputWith400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordsAreStoredAsBCryptHashes() {
        String hash = passwordEncoder.encode("s3cret-pass");
        assertThat(hash).startsWith("$2").doesNotContain("s3cret-pass");
        assertThat(passwordEncoder.matches("s3cret-pass", hash)).isTrue();
    }
}
