package com.encore.encoreapi.security;

import com.encore.encoreapi.user.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final boolean secureCookie;
    private final String sameSite;

    public AuthController(UserService userService, UserRepository userRepository, JwtService jwtService,
                          @Value("${auth.cookie.secure:true}") boolean secureCookie,
                          @Value("${auth.cookie.same-site:Lax}") String sameSite) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
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
        if (response.isRequiresTotp()) {
            return ResponseEntity.ok(Map.of("requiresTotp", true));
        }
        return authenticatedResponse(response);
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

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.updateName(UUID.fromString(userId), request.getName());
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
        return authenticatedResponse(response);
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = sessionCookie("", 0);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    private ResponseEntity<Map<String, Object>> authenticatedResponse(LoginResponse response) {
        ResponseCookie cookie = sessionCookie(response.getToken(), jwtService.getExpiration().toSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "email", response.getEmail(),
                        "name", response.getName(),
                        "requiresTotp", false
                ));
    }

    private ResponseCookie sessionCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("encore_session", token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/api")
                .maxAge(maxAgeSeconds)
                .build();
    }
}