package com.encore.encoreapi.user;

import com.encore.encoreapi.email.EmailService;
import com.encore.encoreapi.security.JwtService;
import com.encore.encoreapi.security.TotpService;
import com.encore.encoreapi.security.TotpSetupResponse;
import com.encore.encoreapi.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final LoginAttemptService loginAttemptService;

    public UserService(UserRepository userRepository,
                       VerificationTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       JwtService jwtService,
                       TotpService totpService,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.loginAttemptService = loginAttemptService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe una cuenta con este email");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName()
        );
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(
                token,
                user,
                LocalDateTime.now().plusHours(24)
        );
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);

        return user;
    }

    public void verifyUser(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (verificationToken.isExpired()) {
            throw new IllegalArgumentException("El token ha expirado");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);
    }

    public LoginResponse login(LoginRequest request) {
        if (loginAttemptService.isBlocked(request.getEmail())) {
            throw new IllegalArgumentException("Demasiados intentos fallidos. Inténtalo de nuevo en unos minutos.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getEmail());
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Debes verificar tu correo antes de iniciar sesión");
        }

        loginAttemptService.loginSucceeded(request.getEmail());

        if (user.isTotpEnabled()) {
            return LoginResponse.requiresTotp();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getEmail(), user.getName());
    }

    public LoginResponse loginWithTotp(LoginRequest request, String totpCode) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
            throw new IllegalArgumentException("2FA no está activado para este usuario");
        }

        if (!totpService.verifyCode(user.getTotpSecret(), totpCode)) {
            throw new IllegalArgumentException("Código 2FA inválido");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getEmail(), user.getName());
    }

    public TotpSetupResponse setupTotp(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        String qrImage = totpService.generateQrCodeImage(secret, user.getEmail());
        return new TotpSetupResponse(qrImage, secret);
    }

    public void confirmTotp(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getTotpSecret() == null) {
            throw new IllegalArgumentException("Primero debes iniciar la configuración de 2FA");
        }

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new IllegalArgumentException("Código inválido");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public User updateName(UUID userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setName(name.strip());
        return userRepository.save(user);
    }
}