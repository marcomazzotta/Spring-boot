package com.example.esame.controller;

import com.example.esame.model.Trip;
import com.example.esame.model.User;
import com.example.esame.repository.TripRepository;
import com.example.esame.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public TripController(TripRepository tripRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    // ✅ GET /trips?origin=&destination=
    // Pubblico: tutti possono vedere le corse
    @GetMapping
    public ResponseEntity<List<Trip>> getTrips(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination
    ) {
        if (origin == null && destination == null) {
            return ResponseEntity.ok(tripRepository.findAll());
        }
        return ResponseEntity.ok(tripRepository.searchTrips(origin, destination));
    }

    // ✅ POST /trips (crea nuova corsa)
    // Solo ADMIN può creare nuove corse
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createTrip(@RequestBody Trip trip) {
        if (trip.getOrigin() == null || trip.getDestination() == null || trip.getDepartureTime() == null) {
            return ResponseEntity.badRequest().body("Dati corsa incompleti");
        }

        if (trip.getPrice() == null || trip.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body("Prezzo non valido");
        }

        Trip saved = tripRepository.save(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ✅ POST /trips/{tripId}/buy
    // Solo utenti autenticati (USER o ADMIN)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Transactional
    @PostMapping("/{tripId}/buy")
    public ResponseEntity<?> buyTrip(@PathVariable Integer tripId, Authentication authentication) {
        String email = authentication.getName();

        // Trova l’utente dal token
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // Trova la corsa
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Corsa non trovata"));

        // Controlla credito
        if (user.getCredit().compareTo(trip.getPrice()) < 0) {
            return ResponseEntity.status(422).body("Credito insufficiente");
        }

        // Scala il credito e salva
        user.setCredit(user.getCredit().subtract(trip.getPrice()));
        userRepository.save(user);

        // Restituisce ricevuta
        BuyReceipt receipt = new BuyReceipt(
                user.getId(),
                trip.getId(),
                trip.getPrice(),
                user.getCredit()
        );

        return ResponseEntity.ok(receipt);
    }

    // 📄 DTO interno per la ricevuta
    private record BuyReceipt(Integer userId, Integer tripId, BigDecimal addebito, BigDecimal saldoResiduo) {}
}
