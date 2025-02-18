package com.productapi.product.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Or Argon2PasswordEncoder, Pbkdf2PasswordEncoder
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {  // This is the method
        http.authorizeHttpRequests((authz) -> authz
                        .mvcMatchers("/api/auth/**").permitAll() // Allow /api/auth without auth
                        .anyRequest().authenticated() // All other requests need auth
                )
                .csrf().disable(); // Disable CSRF (usually for APIs)

        return http.build();
    }
}
