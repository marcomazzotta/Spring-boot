package com.example.esame.controller;

import com.example.esame.model.User;
import com.example.esame.repository.UserRepository;
import com.example.esame.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // ✅ POST /register  →  accetta JSON { email, password, credit, role }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        try {
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            BigDecimal credit = body.get("credit") != null
                    ? new BigDecimal(body.get("credit").toString())
                    : BigDecimal.ZERO;
            String role = body.get("role") != null
                    ? body.get("role").toString().toUpperCase()
                    : "USER";

            User user = userService.register(email, password, credit, role);
            user.setPassword(null); // mai inviare la password, anche se hashata

            return ResponseEntity.status(HttpStatus.CREATED).body(user);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore durante la registrazione");
        }
    }

    // ✅ GET /users/{id}
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Integer id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utente non trovato");
        }
        User user = userOpt.get();
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // ✅ PATCH /users/{id}/credit/topup?amount=50
    @PatchMapping("/users/{id}/credit/topup")
    public ResponseEntity<?> topUp(@PathVariable Integer id,
                                   @RequestParam BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("L'importo deve essere positivo");
        }

        try {
            User updatedUser = userService.addCredit(id, amount);
            updatedUser.setPassword(null);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
