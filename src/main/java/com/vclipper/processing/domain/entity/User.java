package com.vclipper.processing.domain.entity;

/**
 * Simple user reference entity for the processing context
 * Note: Full user management is handled by separate User Profile Service
 */
public record User(String userId, String email) {
    
    /**
     * Validate user data
     */
    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty()
                && email != null && !email.trim().isEmpty()
                && email.contains("@");
    }
}
