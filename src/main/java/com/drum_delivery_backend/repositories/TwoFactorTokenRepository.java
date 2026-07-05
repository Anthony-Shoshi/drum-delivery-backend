package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.TwoFactorToken;
import com.drum_delivery_backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TwoFactorTokenRepository extends JpaRepository<TwoFactorToken, Long> {
    
    /**
     * Find a valid token for a user and purpose
     */
    Optional<TwoFactorToken> findByUserAndTokenAndPurposeAndVerifiedFalse(
        User user, String token, TwoFactorToken.TokenPurpose purpose);
    
    /**
     * Find the latest unverified token for a user and purpose
     */
    Optional<TwoFactorToken> findFirstByUserAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
        User user, TwoFactorToken.TokenPurpose purpose);
    
    /**
     * Find all unverified tokens for a user and purpose
     */
    List<TwoFactorToken> findByUserAndPurposeAndVerifiedFalse(
        User user, TwoFactorToken.TokenPurpose purpose);
    
    /**
     * Count unverified tokens for a user and purpose within a time period
     */
    @Query("SELECT COUNT(t) FROM TwoFactorToken t WHERE t.user = :user AND t.purpose = :purpose " +
           "AND t.verified = false AND t.createdAt >= :since")
    long countUnverifiedTokensSince(@Param("user") User user, 
                                   @Param("purpose") TwoFactorToken.TokenPurpose purpose,
                                   @Param("since") LocalDateTime since);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM TwoFactorToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Delete all unverified tokens for a user and purpose
     */
    @Modifying
    @Query("DELETE FROM TwoFactorToken t WHERE t.user = :user AND t.purpose = :purpose AND t.verified = false")
    int deleteUnverifiedTokensForUser(@Param("user") User user, 
                                     @Param("purpose") TwoFactorToken.TokenPurpose purpose);
    
    /**
     * Mark a token as verified
     */
    @Modifying
    @Query("UPDATE TwoFactorToken t SET t.verified = true, t.verifiedAt = :verifiedAt WHERE t.id = :tokenId")
    int markTokenAsVerified(@Param("tokenId") Long tokenId, @Param("verifiedAt") LocalDateTime verifiedAt);
    
    /**
     * Find tokens that need cleanup (expired or old verified tokens)
     */
    @Query("SELECT t FROM TwoFactorToken t WHERE t.expiresAt < :now OR " +
           "(t.verified = true AND t.verifiedAt < :cleanupBefore)")
    List<TwoFactorToken> findTokensForCleanup(@Param("now") LocalDateTime now, 
                                             @Param("cleanupBefore") LocalDateTime cleanupBefore);
}