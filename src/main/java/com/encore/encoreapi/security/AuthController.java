package com.encore.encoreapi.security;

import com.encore.encoreapi.user.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new UserResponse(user.getId(), user.getEmail(), user.getName())
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        userService.verifyUser(token);
        return ResponseEntity.ok("Cuenta verificada correctamente. Ya puedes iniciar sesión.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }

        String userId = (String) authentication.getPrincipal();
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getName()));
    }
    @PostMapping("/totp/setup")
    public ResponseEntity<?> setupTotp() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        TotpSetupResponse response = userService.setupTotp(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/totp/confirm")
    public ResponseEntity<?> confirmTotp(@Valid @RequestBody TotpVerifyRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.confirmTotp(UUID.fromString(userId), request.getCode());
        return ResponseEntity.ok("2FA activado correctamente.");
    }
    @PostMapping("/login/totp")
    public ResponseEntity<?> loginWithTotp(@RequestParam String code, @Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.loginWithTotp(request, code);
        return ResponseEntity.ok(response);
    }
}