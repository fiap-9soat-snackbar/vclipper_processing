package com.vclipper.processing.infrastructure.adapters.user;

import com.vclipper.processing.application.ports.UserServicePort;
import com.vclipper.processing.domain.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of User Service for development and testing
 * Simulates user service operations with predefined test users
 */
public class MockUserServiceAdapter implements UserServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(MockUserServiceAdapter.class);
    
    // Predefined test users for development
    private final Map<String, User> testUsers = new ConcurrentHashMap<>();
    
    public MockUserServiceAdapter() {
        // Initialize with test users
        initializeTestUsers();
    }
    
    private void initializeTestUsers() {
        testUsers.put("test-user-123", new User("test-user-123", "test@vclipper.dev"));
        testUsers.put("admin-user-456", new User("admin-user-456", "admin@vclipper.dev"));
        testUsers.put("demo-user-789", new User("demo-user-789", "demo@vclipper.dev"));
        testUsers.put("john-doe-001", new User("john-doe-001", "john.doe@example.com"));
        testUsers.put("jane-smith-002", new User("jane-smith-002", "jane.smith@example.com"));
        
        logger.info("🧑‍💼 MOCK USER SERVICE: Initialized with {} test users", testUsers.size());
        testUsers.values().forEach(user -> 
            logger.debug("   👤 User: {} ({})", user.userId(), user.email())
        );
    }
    
    @Override
    public Optional<User> findById(String userId) {
        logger.debug("🔍 MOCK USER SERVICE: Finding user by ID: {}", userId);
        
        // In integration testing and production, we trust API Gateway JWT validation
        // Create user on-demand for any valid Cognito user ID
        if (userId != null && !userId.trim().isEmpty()) {
            String email = generateRealisticEmail(userId);
            User user = new User(userId, email);
            logger.debug("   ✅ Created user on-demand: {} ({})", user.userId(), user.email());
            return Optional.of(user);
        } else {
            logger.debug("   ❌ Invalid user ID provided: {}", userId);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean isActiveUser(String userId) {
        logger.debug("🔍 MOCK USER SERVICE: Checking if user is active: {}", userId);
        
        // In production, API Gateway validates JWT tokens before forwarding requests
        // So we trust any user ID that reaches the backend
        boolean isActive = userId != null && !userId.trim().isEmpty();
        
        if (isActive) {
            logger.debug("   ✅ User is trusted from API Gateway: {}", userId);
        } else {
            logger.debug("   ❌ Invalid user ID: {}", userId);
        }
        
        return isActive;
    }
    
    @Override
    public Optional<String> getUserEmail(String userId) {
        logger.debug("📧 MOCK USER SERVICE: Getting email for user: {}", userId);
        
        // Generate realistic email for any valid user ID
        if (userId != null && !userId.trim().isEmpty()) {
            String email = generateRealisticEmail(userId);
            logger.debug("   ✅ Generated email: {} for user: {}", email, userId);
            return Optional.of(email);
        } else {
            logger.debug("   ❌ Invalid user ID: {}", userId);
            return Optional.empty();
        }
    }
    
    /**
     * Generate a realistic email address for Cognito users
     * In production, this would come from Cognito user attributes via API Gateway
     */
    private String generateRealisticEmail(String userId) {
        // For Cognito UUIDs, create a more realistic email
        if (userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            // Use first 8 characters of UUID for a cleaner email
            String shortId = userId.substring(0, 8);
            return "user-" + shortId + "@vclipper.app";
        } else {
            // For other user IDs, use as-is
            return userId + "@vclipper.app";
        }
    }
    
    @Override
    public boolean isHealthy() {
        logger.debug("💚 MOCK USER SERVICE: Health check - Always healthy in mock mode");
        return true;
    }
    
    /**
     * Add a test user (for testing purposes)
     */
    public void addTestUser(String userId, String email) {
        logger.info("➕ MOCK USER SERVICE: Adding test user: {} ({})", userId, email);
        testUsers.put(userId, new User(userId, email));
    }
    
    /**
     * Remove a test user (for testing purposes)
     */
    public void removeTestUser(String userId) {
        logger.info("➖ MOCK USER SERVICE: Removing test user: {}", userId);
        testUsers.remove(userId);
    }
    
    /**
     * Get all test users (for debugging)
     */
    public Map<String, User> getAllTestUsers() {
        logger.debug("📋 MOCK USER SERVICE: Returning all {} test users", testUsers.size());
        return Map.copyOf(testUsers);
    }
    
    /**
     * Clear all test users and reinitialize (for testing)
     */
    public void resetTestUsers() {
        logger.info("🔄 MOCK USER SERVICE: Resetting test users");
        testUsers.clear();
        initializeTestUsers();
    }
}
