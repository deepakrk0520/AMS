package com.shopsphere.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protects {@code /actuator/metrics} and {@code /actuator/prometheus} with HTTP Basic
 * so Prometheus can scrape without a JWT while the data is not public. Uses its own
 * single in-memory user, separate from application users. If no password is configured
 * the user does not exist and these endpoints reject everyone.
 */
@Configuration
public class ActuatorSecurityConfig {

    static final String ACTUATOR_ROLE = "ACTUATOR";

    @Bean
    @Order(1)
    SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder,
            @Value("${app.security.actuator.username}") String username,
            @Value("${app.security.actuator.password}") String password,
            SecurityErrorHandlers errorHandlers) throws Exception {
        InMemoryUserDetailsManager users = password.isBlank()
                ? new InMemoryUserDetailsManager()
                : new InMemoryUserDetailsManager(User.withUsername(username)
                        .password(passwordEncoder.encode(password)).roles(ACTUATOR_ROLE).build());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);
        AuthenticationManager manager = new ProviderManager(provider);

        http
                .securityMatcher("/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus")
                .authenticationManager(manager)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ACTUATOR_ROLE))
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        new org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint() {
                            {
                                setRealmName("shopsphere-actuator");
                            }
                        }))
                .exceptionHandling(e -> e.accessDeniedHandler(errorHandlers))
                .cors(Customizer.withDefaults());
        return http.build();
    }
}
