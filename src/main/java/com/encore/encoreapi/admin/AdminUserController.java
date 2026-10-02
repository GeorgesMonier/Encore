package com.encore.encoreapi.admin;

import com.encore.encoreapi.user.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> allUsers() {
        List<AdminUserResponse> users = userRepository.findAll().stream()
                .map(AdminUserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateRole(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setRole(request.getRole());
        userRepository.save(user);

        return ResponseEntity.ok(AdminUserResponse.from(user));
    }
}