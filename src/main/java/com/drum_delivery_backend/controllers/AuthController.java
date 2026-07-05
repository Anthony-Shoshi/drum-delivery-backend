package com.drum_delivery_backend.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drum_delivery_backend.models.ERole;
import com.drum_delivery_backend.models.Role;
import com.drum_delivery_backend.models.User;
import com.drum_delivery_backend.payload.request.LoginRequest;
import com.drum_delivery_backend.payload.request.PasswordChangeRequest;
import com.drum_delivery_backend.payload.request.ResendTokenRequest;
import com.drum_delivery_backend.payload.request.SignupRequest;
import com.drum_delivery_backend.payload.request.TwoFactorVerificationRequest;
import com.drum_delivery_backend.payload.response.JwtResponse;
import com.drum_delivery_backend.payload.response.MessageResponse;
import com.drum_delivery_backend.payload.response.TwoFactorRequiredResponse;
import com.drum_delivery_backend.payload.response.UserInfoResponse;
import com.drum_delivery_backend.repositories.RoleRepository;
import com.drum_delivery_backend.repositories.UserRepository;
import com.drum_delivery_backend.security.jwt.JwtUtils;
import com.drum_delivery_backend.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;
    
    @Autowired
    private com.drum_delivery_backend.services.TwoFactorService twoFactorService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for username: {}", loginRequest.getUsername());
        
        try {
            // Log authentication attempt
            logger.debug("Attempting authentication for user: {}", loginRequest.getUsername());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            logger.info("Authentication successful for user: {}", loginRequest.getUsername());
            
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            // Get user entity to check 2FA requirement
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Update last login attempt timestamp
            user.setLastLoginAttempt(java.time.LocalDateTime.now());
            userRepository.save(user);
            
            // Check if 2FA is enabled for this user
            if (twoFactorService.is2FAEnabled(user)) {
                // Generate and send 2FA code
                try {
                    twoFactorService.generateLogin2FAToken(user);
                    logger.info("2FA token sent for user: {}", loginRequest.getUsername());
                    
                    return ResponseEntity.ok(new TwoFactorRequiredResponse(
                        "Two-factor authentication required. Please check your email for verification code.", 
                        userDetails.getUsername(), 
                        true));
                } catch (Exception e) {
                    logger.error("Failed to send 2FA token for user: {}", loginRequest.getUsername(), e);
                    return ResponseEntity.status(500)
                            .body(new MessageResponse("Error: Failed to send verification code. Please try again."));
                }
            }
            
            // If 2FA is not enabled, proceed with normal login
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            logger.debug("User roles: {}", roles);

            Boolean passwordChangeRequired = user.getPasswordChangeRequired();

            logger.info("Login successful for user: {} with roles: {}", loginRequest.getUsername(), roles);
            
            return ResponseEntity.ok(JwtResponse.builder()
                                          .accessToken(jwt)
                                          .tokenType("Bearer")
                                          .refreshToken(null)
                                          .id(userDetails.getId())
                                          .username(userDetails.getUsername())
                                          .email(userDetails.getEmail())
                                          .firstName(userDetails.getFirstName())
                                          .lastName(userDetails.getLastName())
                                          .roles(roles)
                                          .passwordChangeRequired(passwordChangeRequired)
                                          .build());
        } catch (Exception e) {
            logger.error("Authentication failed for user: {} - Error: {}", loginRequest.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(), 
                             signUpRequest.getEmail(),
                             encoder.encode(signUpRequest.getPassword()),
                             signUpRequest.getFirstName(),
                             signUpRequest.getLastName());

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                case "admin":
                    Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(adminRole);
                    break;
                case "manager":
                    Role modRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(modRole);
                    break;
                case "operator":
                    Role operatorRole = roleRepository.findByName(ERole.ROLE_OPERATOR)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(operatorRole);
                    break;
                default:
                    Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // For stateless JWT authentication, logout is primarily handled on the frontend
        // by removing the token. This endpoint confirms successful logout.
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("User logged out successfully!"));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                roles));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest passwordChangeRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Get current user
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        // Verify current password
        if (!encoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Current password is incorrect!"));
        }

        // Check if new password is different from current password
        if (encoder.matches(passwordChangeRequest.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: New password must be different from current password!"));
        }

        // Update password
        user.setPassword(encoder.encode(passwordChangeRequest.getNewPassword()));
        user.setPasswordChangeRequired(false); // Reset password change requirement
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Password changed successfully!"));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@Valid @RequestBody TwoFactorVerificationRequest verificationRequest) {
        logger.info("2FA verification attempt for username: {}", verificationRequest.getUsername());
        
        try {
            // Find user
            User user = userRepository.findByUsername(verificationRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify the 2FA token
            boolean isValid = twoFactorService.verify2FAToken(user, verificationRequest.getCode(), 
                    com.drum_delivery_backend.models.TwoFactorToken.TokenPurpose.LOGIN);
            
            if (!isValid) {
                logger.warn("Invalid 2FA token for user: {}", verificationRequest.getUsername());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid or expired verification code!"));
            }
            
            // Create authentication token for the verified user
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());
            
            Boolean passwordChangeRequired = user.getPasswordChangeRequired();
            
            logger.info("2FA verification successful for user: {}", verificationRequest.getUsername());
            
            return ResponseEntity.ok(JwtResponse.builder()
                                          .accessToken(jwt)
                                          .tokenType("Bearer")
                                          .refreshToken(null)
                                          .id(userDetails.getId())
                                          .username(userDetails.getUsername())
                                          .email(userDetails.getEmail())
                                          .firstName(userDetails.getFirstName())
                                          .lastName(userDetails.getLastName())
                                          .roles(roles)
                                          .passwordChangeRequired(passwordChangeRequired)
                                          .build());
                                          
        } catch (Exception e) {
            logger.error("2FA verification failed for user: {} - Error: {}", 
                    verificationRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Verification failed. Please try again."));
        }
    }

    @PostMapping("/resend-2fa")
    public ResponseEntity<?> resend2FA(@Valid @RequestBody ResendTokenRequest resendRequest) {
        logger.info("2FA resend request for username: {}", resendRequest.getUsername());
        
        try {
            // Find user
            User user = userRepository.findByUsername(resendRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if 2FA is enabled
            if (!twoFactorService.is2FAEnabled(user)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Two-factor authentication is not enabled for this user."));
            }
            
            // Resend 2FA token (with rate limiting)
            boolean sent = twoFactorService.resend2FAToken(user);
            
            if (!sent) {
                logger.warn("2FA resend rate limit exceeded for user: {}", resendRequest.getUsername());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Too many requests. Please wait before requesting another code."));
            }
            
            logger.info("2FA token resent for user: {}", resendRequest.getUsername());
            return ResponseEntity.ok(new MessageResponse("Verification code sent successfully!"));
            
        } catch (Exception e) {
            logger.error("2FA resend failed for user: {} - Error: {}", 
                    resendRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new MessageResponse("Error: Failed to send verification code. Please try again."));
        }
    }
}