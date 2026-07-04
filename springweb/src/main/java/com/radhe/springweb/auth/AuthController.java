package com.radhe.springweb.auth;

import com.radhe.springweb.auth.dto.AuthResponse;
import com.radhe.springweb.auth.dto.LoginRequest;
import com.radhe.springweb.auth.dto.RegisterRequest;
import com.radhe.springweb.security.JwtService;
import com.radhe.springweb.user.LoginHistory;
import com.radhe.springweb.user.LoginHistoryRepository;
import com.radhe.springweb.user.User;
import com.radhe.springweb.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                           LoginHistoryRepository loginHistoryRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "An account with this email already exists"));
        }

        User user = new User(request.getName(), request.getEmail(),
                passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getId(), user.getName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User vanished after authentication"));

                // Record login history best-effort so authentication never fails because of auditing.
                try {
                        String ip = httpRequest.getHeader("X-Forwarded-For") != null
                                        ? httpRequest.getHeader("X-Forwarded-For")
                                        : httpRequest.getRemoteAddr();
                        String userAgent = sanitizeUserAgent(httpRequest.getHeader("User-Agent"));
                        loginHistoryRepository.save(new LoginHistory(user, ip, userAgent));
                } catch (Exception ignored) {
                        // Login should still succeed even if audit logging cannot be written.
                }

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getName(), user.getEmail()));
    }

        private String sanitizeUserAgent(String userAgent) {
                if (userAgent == null) {
                        return null;
                }
                return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
        }
}
