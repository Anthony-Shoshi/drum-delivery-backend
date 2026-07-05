package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.User;
import com.drum_delivery_backend.payload.request.PasswordResetConfirmRequest;
import com.drum_delivery_backend.payload.response.MessageResponse;
import com.drum_delivery_backend.repositories.UserRepository;
import com.drum_delivery_backend.services.TwoFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/public")
public class PublicAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(PublicAuthController.class);
    
    @Autowired
    private TwoFactorService twoFactorService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest confirmRequest) {
        logger.info("Password reset confirmation attempt with token");
        
        try {
            // Find all users and check which one has this token
            List<User> allUsers = userRepository.findAll();
            User userWithToken = null;
            
            for (User user : allUsers) {
                boolean isValid = twoFactorService.verify2FAToken(user, confirmRequest.getToken(),
                        com.drum_delivery_backend.models.TwoFactorToken.TokenPurpose.PASSWORD_RESET);
                if (isValid) {
                    userWithToken = user;
                    break;
                }
            }
            
            if (userWithToken == null) {
                logger.warn("Invalid or expired password reset token provided");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid or expired reset token!"));
            }
            
            // Update password
            userWithToken.setPassword(passwordEncoder.encode(confirmRequest.getNewPassword()));
            userWithToken.setPasswordChangeRequired(false);
            userRepository.save(userWithToken);
            
            logger.info("Password reset successful for user: {}", userWithToken.getUsername());
            
            // Send confirmation email if needed
            try {
                String userName = userWithToken.getFirstName() != null ? userWithToken.getFirstName() : userWithToken.getUsername();
                // We could implement email confirmation here if needed
                // emailService.sendPasswordResetConfirmation(userWithToken.getEmail(), userName);
            } catch (Exception e) {
                // Log but don't fail the password reset
                logger.error("Failed to send confirmation email: {}", e.getMessage());
            }
            
            return ResponseEntity.ok(new MessageResponse("Password reset successful! You can now log in with your new password."));
            
        } catch (Exception e) {
            logger.error("Password reset confirmation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Failed to reset password. Please try again."));
        }
    }
}