package com.drum_delivery_backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "two_factor_tokens")
public class TwoFactorToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank(message = "Token is required")
    @Size(min = 6, max = 6, message = "Token must be exactly 6 characters")
    @Column(name = "token", nullable = false)
    private String token;
    
    @NotNull(message = "Token purpose is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private TokenPurpose purpose;
    
    @NotNull(message = "Expiry time is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "verified", nullable = false)
    private boolean verified = false;
    
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;
    
    @NotNull(message = "Creation time is required")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    public enum TokenPurpose {
        LOGIN,
        PASSWORD_RESET
    }
    
    public TwoFactorToken() {
        this.createdAt = LocalDateTime.now();
    }
    
    public TwoFactorToken(User user, String token, TokenPurpose purpose, LocalDateTime expiresAt) {
        this();
        this.user = user;
        this.token = token;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public TokenPurpose getPurpose() {
        return purpose;
    }
    
    public void setPurpose(TokenPurpose purpose) {
        this.purpose = purpose;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void setVerified(boolean verified) {
        this.verified = verified;
        if (verified && this.verifiedAt == null) {
            this.verifiedAt = LocalDateTime.now();
        }
    }
    
    public int getAttempts() {
        return attempts;
    }
    
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
    
    public void incrementAttempts() {
        this.attempts++;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
    
    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    public boolean isValid() {
        return !isExpired() && !verified;
    }
}