package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.TwoFactorToken;
import com.drum_delivery_backend.models.User;
import com.drum_delivery_backend.repositories.TwoFactorTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TwoFactorService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorService.class);
    
    private final TwoFactorTokenRepository tokenRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom;
    
    @Value("${app.security.two-factor.token-length:6}")
    private int tokenLength;
    
    @Value("${app.security.two-factor.token-expiry-minutes:5}")
    private int tokenExpiryMinutes;
    
    @Value("${app.security.two-factor.max-attempts:3}")
    private int maxAttempts;
    
    public TwoFactorService(TwoFactorTokenRepository tokenRepository, EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Generate a random numeric token
     */
    private String generateToken() {
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < tokenLength; i++) {
            token.append(secureRandom.nextInt(10));
        }
        return token.toString();
    }
    
    /**
     * Generate and send a 2FA token for login
     */
    @Transactional
    public String generateLogin2FAToken(User user) {
        // Invalidate any existing unverified login tokens
        tokenRepository.deleteUnverifiedTokensForUser(user, TwoFactorToken.TokenPurpose.LOGIN);
        
        // Generate new token
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpiryMinutes);
        
        TwoFactorToken twoFactorToken = new TwoFactorToken(user, token, TwoFactorToken.TokenPurpose.LOGIN, expiresAt);
        tokenRepository.save(twoFactorToken);
        
        // Send email
        try {
            String userName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
            emailService.send2FACode(user.getEmail(), userName, token);
            logger.info("2FA login token sent to user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to send 2FA token email to user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to send 2FA code via email", e);
        }
        
        return token; // Return for testing purposes, don't return in production
    }
    
    /**
     * Generate and send a password reset token
     */
    @Transactional
    public String generatePasswordResetToken(User user) {
        // Invalidate any existing unverified password reset tokens
        tokenRepository.deleteUnverifiedTokensForUser(user, TwoFactorToken.TokenPurpose.PASSWORD_RESET);
        
        // Generate new token (longer for password reset)
        String token = generateToken() + generateToken(); // 12 digits for password reset
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24); // 24 hours for password reset
        
        TwoFactorToken resetToken = new TwoFactorToken(user, token, TwoFactorToken.TokenPurpose.PASSWORD_RESET, expiresAt);
        tokenRepository.save(resetToken);
        
        // Send email
        try {
            String userName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
            emailService.sendPasswordResetEmail(user.getEmail(), userName, token);
            logger.info("Password reset token sent to user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to send password reset email to user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
        
        return token;
    }
    
    /**
     * Verify a 2FA token
     */
    @Transactional
    public boolean verify2FAToken(User user, String token, TwoFactorToken.TokenPurpose purpose) {
        Optional<TwoFactorToken> tokenOpt = tokenRepository
            .findByUserAndTokenAndPurposeAndVerifiedFalse(user, token, purpose);
        
        if (tokenOpt.isEmpty()) {
            logger.warn("Invalid or already used token for user: {} purpose: {}", user.getUsername(), purpose);
            return false;
        }
        
        TwoFactorToken twoFactorToken = tokenOpt.get();
        
        // Check if token is expired
        if (twoFactorToken.isExpired()) {
            logger.warn("Expired token for user: {} purpose: {}", user.getUsername(), purpose);
            return false;
        }
        
        // Check attempts
        if (twoFactorToken.getAttempts() >= maxAttempts) {
            logger.warn("Too many attempts for token for user: {} purpose: {}", user.getUsername(), purpose);
            return false;
        }
        
        // Increment attempts
        twoFactorToken.incrementAttempts();
        
        // Mark as verified
        twoFactorToken.setVerified(true);
        tokenRepository.save(twoFactorToken);
        
        logger.info("Successfully verified {} token for user: {}", purpose, user.getUsername());
        return true;
    }
    
    /**
     * Check if user has exceeded rate limits for token generation
     */
    public boolean hasExceededRateLimit(User user, TwoFactorToken.TokenPurpose purpose) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long tokenCount = tokenRepository.countUnverifiedTokensSince(user, purpose, oneHourAgo);
        
        int hourlyLimit = purpose == TwoFactorToken.TokenPurpose.LOGIN ? 10 : 3; // Different limits for different purposes
        return tokenCount >= hourlyLimit;
    }
    
    /**
     * Resend 2FA token (with rate limiting)
     */
    @Transactional
    public boolean resend2FAToken(User user) {
        if (hasExceededRateLimit(user, TwoFactorToken.TokenPurpose.LOGIN)) {
            logger.warn("Rate limit exceeded for user: {} attempting to resend 2FA token", user.getUsername());
            return false;
        }
        
        generateLogin2FAToken(user);
        return true;
    }
    
    /**
     * Enable 2FA for a user
     */
    @Transactional
    public boolean enable2FA(User user) {
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            return false; // Already enabled
        }
        
        user.setTwoFactorEnabled(true);
        logger.info("2FA enabled for user: {}", user.getUsername());
        return true;
    }
    
    /**
     * Disable 2FA for a user
     */
    @Transactional
    public boolean disable2FA(User user) {
        if (Boolean.FALSE.equals(user.getTwoFactorEnabled())) {
            return false; // Already disabled
        }
        
        user.setTwoFactorEnabled(false);
        // Clean up any pending tokens
        tokenRepository.deleteUnverifiedTokensForUser(user, TwoFactorToken.TokenPurpose.LOGIN);
        logger.info("2FA disabled for user: {}", user.getUsername());
        return true;
    }
    
    /**
     * Check if user has 2FA enabled
     */
    public boolean is2FAEnabled(User user) {
        return Boolean.TRUE.equals(user.getTwoFactorEnabled());
    }
    
    /**
     * Cleanup expired tokens (scheduled task)
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cleanupBefore = now.minusDays(7); // Clean verified tokens older than 7 days
            
            List<TwoFactorToken> tokensToCleanup = tokenRepository.findTokensForCleanup(now, cleanupBefore);
            
            if (!tokensToCleanup.isEmpty()) {
                tokenRepository.deleteAll(tokensToCleanup);
                logger.info("Cleaned up {} expired/old tokens", tokensToCleanup.size());
            }
        } catch (Exception e) {
            logger.error("Error during token cleanup", e);
        }
    }
}