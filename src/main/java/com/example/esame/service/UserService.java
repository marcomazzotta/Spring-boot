package com.example.esame.service;

import com.example.esame.model.Role;
import com.example.esame.model.User;
import com.example.esame.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ REGISTRAZIONE con ruolo opzionale (USER o ADMIN)
    public User register(String email, String rawPassword, BigDecimal initialCredit, String roleStr) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email obbligatoria");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password obbligatoria");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email già registrata");
        }

        if (initialCredit == null || initialCredit.compareTo(BigDecimal.ZERO) < 0) {
            initialCredit = BigDecimal.ZERO;
        }

        // ✅ converte la stringa in enum Role (default USER)
        Role role = Role.USER;
        if (roleStr != null && roleStr.equalsIgnoreCase("ADMIN")) {
            role = Role.ADMIN;
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .credit(initialCredit)
                .role(role)
                .build();

        return userRepository.save(user);
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User addCredit(Integer id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("L'importo deve essere positivo");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        user.setCredit(user.getCredit().add(amount));
        return user;
    }
}
