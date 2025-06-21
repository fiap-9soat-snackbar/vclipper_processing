package com.vclipper.processing.application.ports;

import com.vclipper.processing.domain.entity.User;

import java.util.Optional;

/**
 * Port interface for user service operations
 * Abstracts user service implementation (User Profile Service, external API, etc.)
 */
public interface UserServicePort {
    
    /**
     * Get user information by user ID
     * 
     * @param userId User identifier
     * @return User information if found
     */
    Optional<User> findById(String userId);
    
    /**
     * Check if user exists and is active
     * 
     * @param userId User identifier
     * @return true if user exists and is active, false otherwise
     */
    boolean isActiveUser(String userId);
    
    /**
     * Get user's email for notifications
     * 
     * @param userId User identifier
     * @return User's email address if found
     */
    Optional<String> getUserEmail(String userId);
    
    /**
     * Check if user service is healthy and accessible
     * 
     * @return true if service is accessible, false otherwise
     */
    boolean isHealthy();
}
