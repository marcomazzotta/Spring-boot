package com.example.esame.config;

import com.example.esame.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // ✅ abilita @PreAuthorize nei controller
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Endpoint pubblici
                        .requestMatchers("/register", "/login", "/h2-console/**").permitAll()

                        // 👁‍🗨 Endpoint pubblici (consultazione corse)
                        .requestMatchers(HttpMethod.GET, "/trips", "/trips/**").permitAll()

                        // 🧾 Acquisto corse → solo utenti autenticati (USER o ADMIN)
                        .requestMatchers(HttpMethod.POST, "/trips/*/buy").hasAnyRole("USER", "ADMIN")

                        // 🚍 Creazione nuova corsa → solo ADMIN
                        .requestMatchers(HttpMethod.POST, "/trips").hasRole("ADMIN")

                        // 👤 Endpoint personale
                        .requestMatchers("/me/**").authenticated()

                        // Tutto il resto richiede autenticazione
                        .anyRequest().authenticated()
                )
                // 🔐 Applica il filtro JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
