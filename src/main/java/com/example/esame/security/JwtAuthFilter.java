package com.example.esame.security;

import com.example.esame.model.User;
import com.example.esame.repository.UserRepository;
import com.example.esame.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // ✅ 1️⃣ Se non c’è header Authorization → passa avanti
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // ✅ 2️⃣ Estrae email e ruolo dal token
        String email = jwtService.extractEmail(token);
        String roleFromToken = jwtService.extractRole(token); // 👈 aggiunto

        // ✅ 3️⃣ Se l’utente non è ancora autenticato nel contesto
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // 👇 Determina il ruolo dal token o dal DB
                String role = (roleFromToken != null) ? roleFromToken : user.getRole().name();
                String authorityName = "ROLE_" + role;

                // ✅ 4️⃣ Crea i dettagli dell’utente con il ruolo corretto
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword()) // obbligatorio anche se non usato
                        .authorities(Collections.singletonList((GrantedAuthority) () -> authorityName))
                        .build();

                // ✅ 5️⃣ Crea il token di autenticazione Spring
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // ✅ 6️⃣ Imposta l’autenticazione nel contesto
                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("✅ Autenticato: " + email + " con ruolo: " + authorityName);
            }
        }

        filterChain.doFilter(request, response);
    }
}
