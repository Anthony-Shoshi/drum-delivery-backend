package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.User;
import com.drum_delivery_backend.models.Role;
import com.drum_delivery_backend.payload.request.PasswordResetRequest;
import com.drum_delivery_backend.payload.response.MessageResponse;
import com.drum_delivery_backend.services.UserService;
import com.drum_delivery_backend.services.TwoFactorService;
import com.drum_delivery_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    
    @Autowired
    private TwoFactorService twoFactorService;
    
    @Autowired
    private UserRepository userRepository;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> usersPage = userService.getAllUsers(pageable);
        
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(usersPage.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(usersPage.getTotalPages()))
                .body(usersPage.getContent());
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsersNoPagination() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation Error", "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        try {
            return userService.updateUser(id, userDetails)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation Error", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(userService.getAllRoles());
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<?> updateUserPassword(
            @PathVariable Long id, 
            @RequestBody Map<String, String> passwordData) {
        
        String newPassword = passwordData.get("password");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation Error", "message", "Password is required"));
        }

        try {
            User updatedUser = userService.updateUserPassword(id, newPassword);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation Error", "message", e.getMessage()));
        }
    }

    @GetMapping("/exists/username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameExists(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUserCount() {
        long count = userService.getUserCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/password-reset/initiate")
    public ResponseEntity<?> initiatePasswordReset(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        try {
            // Find user by email
            User user = userRepository.findByEmail(passwordResetRequest.getEmail())
                    .orElse(null);
            
            // Always return success for security reasons, even if user doesn't exist
            if (user == null) {
                return ResponseEntity.ok(new MessageResponse("If an account exists with this email, a password reset link has been sent."));
            }
            
            // Check rate limiting
            if (twoFactorService.hasExceededRateLimit(user, com.drum_delivery_backend.models.TwoFactorToken.TokenPurpose.PASSWORD_RESET)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Too many password reset requests. Please wait before requesting another reset."));
            }
            
            // Generate and send password reset token
            twoFactorService.generatePasswordResetToken(user);
            
            return ResponseEntity.ok(new MessageResponse("If an account exists with this email, a password reset link has been sent."));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new MessageResponse("Error: Failed to process password reset request. Please try again."));
        }
    }


    @PostMapping("/{id}/enable-2fa")
    public ResponseEntity<?> enable2FA(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean enabled = twoFactorService.enable2FA(user);
            
            if (!enabled) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Two-factor authentication is already enabled for this user."));
            }
            
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("Two-factor authentication enabled successfully."));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Failed to enable two-factor authentication."));
        }
    }

    @PostMapping("/{id}/disable-2fa")
    public ResponseEntity<?> disable2FA(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean disabled = twoFactorService.disable2FA(user);
            
            if (!disabled) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Two-factor authentication is already disabled for this user."));
            }
            
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("Two-factor authentication disabled successfully."));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Failed to disable two-factor authentication."));
        }
    }
}