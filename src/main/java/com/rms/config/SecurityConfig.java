package com.rms.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Development security: HTTP Basic with in-memory users (maker / checker / head).
 * For production (on-prem) replace with an OAuth2 resource server against Keycloak / AD:
 * http.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())) and map roles to the same authorities.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            // 401 without WWW-Authenticate so browsers do not show their own login popup
            .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized")));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder pe) {
        String pw = pe.encode("Passw0rd!");
        return new InMemoryUserDetailsManager(
            User.withUsername("maker.ravi").password(pw).authorities("TREATY_VIEW", "TREATY_MAKE", "DISTRIBUTION_MAKE").build(),
            User.withUsername("maker.divya").password(pw).authorities("TREATY_VIEW", "TREATY_MAKE", "DISTRIBUTION_MAKE").build(),
            User.withUsername("checker.meena").password(pw).authorities("TREATY_VIEW", "TREATY_CHECK").build(),
            User.withUsername("head.sundar").password(pw).authorities("TREATY_VIEW", "TREATY_CHECK", "TREATY_APPROVE_FINAL").build(),
            User.withUsername("viewer.anil").password(pw).authorities("TREATY_VIEW").build());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${rms.cors.allowed-origins}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
