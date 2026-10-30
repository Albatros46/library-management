package com.library.auth;

import com.library.auth.Dtos.CreateUserRequest;
import com.library.auth.Dtos.LoginRequest;
import com.library.auth.Dtos.LoginResponse;
import com.library.auth.Dtos.UserView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final StaffUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(StaffUserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        StaffUser user = users.findByEmailIgnoreCase(req.email())
                .filter(StaffUser::isActive)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-posta veya parola hatalı"));
        return new LoginResponse(jwt.issue(user), UserView.of(user));
    }

    /** Current user; the gateway puts the JWT subject into X-User-Id. */
    @GetMapping("/me")
    public UserView me(@RequestHeader("X-User-Id") String userId) {
        return users.findById(userId).map(UserView::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/users")
    public List<UserView> list() {
        return users.findAll().stream().map(UserView::of).toList();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateUserRequest req,
                           @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta zaten kayıtlı");
        }
        StaffUser u = new StaffUser();
        u.setFullName(req.fullName());
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(req.role());
        return UserView.of(users.save(u));
    }

    @PatchMapping("/users/{id}/active")
    public UserView setActive(@PathVariable String id, @RequestParam boolean value,
                              @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        StaffUser u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        u.setActive(value);
        return UserView.of(users.save(u));
    }

    private static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu işlem için yönetici yetkisi gerekir");
        }
    }
}
