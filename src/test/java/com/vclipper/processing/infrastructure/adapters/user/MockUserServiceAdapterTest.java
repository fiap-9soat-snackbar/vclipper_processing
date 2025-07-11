package com.vclipper.processing.infrastructure.adapters.user;

import com.vclipper.processing.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MockUserServiceAdapter Tests")
class MockUserServiceAdapterTest {

    private MockUserServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockUserServiceAdapter();
    }

    @Nested
    @DisplayName("User Lookup Tests")
    class UserLookupTests {

        @Test
        @DisplayName("Should find user by valid ID and create on-demand")
        void findById_ValidUserId_ShouldReturnUser() {
            // Arrange
            String userId = "test-user-123";

            // Act
            Optional<User> result = adapter.findById(userId);

            // Assert
            assertTrue(result.isPresent(), "User should be found");
            User user = result.get();
            assertEquals(userId, user.userId(), "User ID should match");
            assertEquals("test-user-123@vclipper.app", user.email(), "Email should be generated correctly");
        }

        @Test
        @DisplayName("Should find user by Cognito UUID and generate realistic email")
        void findById_CognitoUuid_ShouldReturnUserWithRealisticEmail() {
            // Arrange
            String cognitoUserId = "12345678-1234-1234-1234-123456789abc";

            // Act
            Optional<User> result = adapter.findById(cognitoUserId);

            // Assert
            assertTrue(result.isPresent(), "User should be found");
            User user = result.get();
            assertEquals(cognitoUserId, user.userId(), "User ID should match");
            assertEquals("user-12345678@vclipper.app", user.email(), "Email should use first 8 chars of UUID");
        }

        @Test
        @DisplayName("Should return empty for null user ID")
        void findById_NullUserId_ShouldReturnEmpty() {
            // Act
            Optional<User> result = adapter.findById(null);

            // Assert
            assertFalse(result.isPresent(), "Should return empty for null user ID");
        }

        @Test
        @DisplayName("Should return empty for empty user ID")
        void findById_EmptyUserId_ShouldReturnEmpty() {
            // Act
            Optional<User> result = adapter.findById("");

            // Assert
            assertFalse(result.isPresent(), "Should return empty for empty user ID");
        }

        @Test
        @DisplayName("Should return empty for whitespace-only user ID")
        void findById_WhitespaceUserId_ShouldReturnEmpty() {
            // Act
            Optional<User> result = adapter.findById("   ");

            // Assert
            assertFalse(result.isPresent(), "Should return empty for whitespace-only user ID");
        }
    }

    @Nested
    @DisplayName("User Validation Tests")
    class UserValidationTests {

        @Test
        @DisplayName("Should return true for valid user ID")
        void isActiveUser_ValidUserId_ShouldReturnTrue() {
            // Arrange
            String userId = "valid-user-123";

            // Act
            boolean result = adapter.isActiveUser(userId);

            // Assert
            assertTrue(result, "Valid user ID should be considered active");
        }

        @Test
        @DisplayName("Should return true for Cognito UUID")
        void isActiveUser_CognitoUuid_ShouldReturnTrue() {
            // Arrange
            String cognitoUserId = "12345678-1234-1234-1234-123456789abc";

            // Act
            boolean result = adapter.isActiveUser(cognitoUserId);

            // Assert
            assertTrue(result, "Cognito UUID should be considered active");
        }

        @Test
        @DisplayName("Should return false for null user ID")
        void isActiveUser_NullUserId_ShouldReturnFalse() {
            // Act
            boolean result = adapter.isActiveUser(null);

            // Assert
            assertFalse(result, "Null user ID should not be considered active");
        }

        @Test
        @DisplayName("Should return false for empty user ID")
        void isActiveUser_EmptyUserId_ShouldReturnFalse() {
            // Act
            boolean result = adapter.isActiveUser("");

            // Assert
            assertFalse(result, "Empty user ID should not be considered active");
        }

        @Test
        @DisplayName("Should return false for whitespace-only user ID")
        void isActiveUser_WhitespaceUserId_ShouldReturnFalse() {
            // Act
            boolean result = adapter.isActiveUser("   ");

            // Assert
            assertFalse(result, "Whitespace-only user ID should not be considered active");
        }
    }

    @Nested
    @DisplayName("Email Generation Tests")
    class EmailGenerationTests {

        @Test
        @DisplayName("Should get email for valid user ID")
        void getUserEmail_ValidUserId_ShouldReturnEmail() {
            // Arrange
            String userId = "test-user-456";

            // Act
            Optional<String> result = adapter.getUserEmail(userId);

            // Assert
            assertTrue(result.isPresent(), "Email should be present for valid user ID");
            assertEquals("test-user-456@vclipper.app", result.get(), "Email should be generated correctly");
        }

        @Test
        @DisplayName("Should get realistic email for Cognito UUID")
        void getUserEmail_CognitoUuid_ShouldReturnRealisticEmail() {
            // Arrange
            String cognitoUserId = "abcdef12-3456-7890-abcd-ef1234567890";

            // Act
            Optional<String> result = adapter.getUserEmail(cognitoUserId);

            // Assert
            assertTrue(result.isPresent(), "Email should be present for Cognito UUID");
            assertEquals("user-abcdef12@vclipper.app", result.get(), "Email should use first 8 chars of UUID");
        }

        @Test
        @DisplayName("Should return empty for null user ID")
        void getUserEmail_NullUserId_ShouldReturnEmpty() {
            // Act
            Optional<String> result = adapter.getUserEmail(null);

            // Assert
            assertFalse(result.isPresent(), "Should return empty for null user ID");
        }

        @Test
        @DisplayName("Should return empty for empty user ID")
        void getUserEmail_EmptyUserId_ShouldReturnEmpty() {
            // Act
            Optional<String> result = adapter.getUserEmail("");

            // Assert
            assertFalse(result.isPresent(), "Should return empty for empty user ID");
        }

        @Test
        @DisplayName("Should return empty for whitespace-only user ID")
        void getUserEmail_WhitespaceUserId_ShouldReturnEmpty() {
            // Act
            Optional<String> result = adapter.getUserEmail("   ");

            // Assert
            assertFalse(result.isPresent(), "Should return empty for whitespace-only user ID");
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should always return healthy")
        void isHealthy_ShouldAlwaysReturnTrue() {
            // Act
            boolean result = adapter.isHealthy();

            // Assert
            assertTrue(result, "Mock service should always be healthy");
        }
    }

    @Nested
    @DisplayName("Test User Management Tests")
    class TestUserManagementTests {

        @Test
        @DisplayName("Should add test user successfully")
        void addTestUser_ValidUserData_ShouldAddUser() {
            // Arrange
            String userId = "new-test-user";
            String email = "new.test@example.com";

            // Act
            adapter.addTestUser(userId, email);
            Map<String, User> allUsers = adapter.getAllTestUsers();

            // Assert
            assertTrue(allUsers.containsKey(userId), "User should be added to test users");
            User addedUser = allUsers.get(userId);
            assertEquals(userId, addedUser.userId(), "User ID should match");
            assertEquals(email, addedUser.email(), "Email should match");
        }

        @Test
        @DisplayName("Should remove test user successfully")
        void removeTestUser_ExistingUser_ShouldRemoveUser() {
            // Arrange
            String userId = "user-to-remove";
            String email = "remove@example.com";
            adapter.addTestUser(userId, email);

            // Act
            adapter.removeTestUser(userId);
            Map<String, User> allUsers = adapter.getAllTestUsers();

            // Assert
            assertFalse(allUsers.containsKey(userId), "User should be removed from test users");
        }

        @Test
        @DisplayName("Should get all test users")
        void getAllTestUsers_ShouldReturnAllUsers() {
            // Act
            Map<String, User> allUsers = adapter.getAllTestUsers();

            // Assert
            assertNotNull(allUsers, "All users map should not be null");
            assertFalse(allUsers.isEmpty(), "Should have predefined test users");
            
            // Verify some predefined users exist
            assertTrue(allUsers.containsKey("test-user-123"), "Should contain predefined test user");
            assertTrue(allUsers.containsKey("admin-user-456"), "Should contain predefined admin user");
            assertTrue(allUsers.containsKey("demo-user-789"), "Should contain predefined demo user");
        }

        @Test
        @DisplayName("Should reset test users to initial state")
        void resetTestUsers_ShouldReinitializeUsers() {
            // Arrange - Add a custom user
            String customUserId = "custom-user";
            adapter.addTestUser(customUserId, "custom@example.com");
            assertTrue(adapter.getAllTestUsers().containsKey(customUserId), "Custom user should be added");

            // Act
            adapter.resetTestUsers();

            // Assert
            Map<String, User> allUsers = adapter.getAllTestUsers();
            assertFalse(allUsers.containsKey(customUserId), "Custom user should be removed after reset");
            
            // Verify predefined users are restored
            assertTrue(allUsers.containsKey("test-user-123"), "Predefined users should be restored");
            assertTrue(allUsers.containsKey("admin-user-456"), "Predefined users should be restored");
            assertTrue(allUsers.containsKey("demo-user-789"), "Predefined users should be restored");
        }

        @Test
        @DisplayName("Should return immutable copy of test users")
        void getAllTestUsers_ShouldReturnImmutableCopy() {
            // Act
            Map<String, User> allUsers = adapter.getAllTestUsers();

            // Assert
            assertThrows(UnsupportedOperationException.class, () -> {
                allUsers.put("new-user", new User("new-user", "new@example.com"));
            }, "Returned map should be immutable");
        }
    }

    @Nested
    @DisplayName("Email Generation Logic Tests")
    class EmailGenerationLogicTests {

        @Test
        @DisplayName("Should generate correct email for regular user ID")
        void generateEmail_RegularUserId_ShouldAppendDomain() {
            // Arrange
            String userId = "john-doe-123";

            // Act
            Optional<String> result = adapter.getUserEmail(userId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("john-doe-123@vclipper.app", result.get());
        }

        @Test
        @DisplayName("Should generate short email for Cognito UUID")
        void generateEmail_CognitoUuid_ShouldUseShortFormat() {
            // Arrange
            String cognitoUserId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

            // Act
            Optional<String> result = adapter.getUserEmail(cognitoUserId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("user-a1b2c3d4@vclipper.app", result.get());
        }

        @Test
        @DisplayName("Should handle edge case Cognito UUID formats")
        void generateEmail_EdgeCaseCognitoUuid_ShouldHandleCorrectly() {
            // Arrange - UUID with all lowercase
            String cognitoUserId = "00000000-1111-2222-3333-444444444444";

            // Act
            Optional<String> result = adapter.getUserEmail(cognitoUserId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("user-00000000@vclipper.app", result.get());
        }
    }

    @Nested
    @DisplayName("Integration Behavior Tests")
    class IntegrationBehaviorTests {

        @Test
        @DisplayName("Should maintain consistency between findById and getUserEmail")
        void consistency_FindByIdAndGetUserEmail_ShouldReturnSameEmail() {
            // Arrange
            String userId = "consistency-test-user";

            // Act
            Optional<User> userResult = adapter.findById(userId);
            Optional<String> emailResult = adapter.getUserEmail(userId);

            // Assert
            assertTrue(userResult.isPresent(), "User should be found");
            assertTrue(emailResult.isPresent(), "Email should be found");
            assertEquals(userResult.get().email(), emailResult.get(), "Emails should be consistent");
        }

        @Test
        @DisplayName("Should maintain consistency between isActiveUser and findById")
        void consistency_IsActiveUserAndFindById_ShouldBeConsistent() {
            // Arrange
            String validUserId = "valid-user";
            String invalidUserId = null;

            // Act & Assert - Valid user
            assertTrue(adapter.isActiveUser(validUserId), "Valid user should be active");
            assertTrue(adapter.findById(validUserId).isPresent(), "Valid user should be found");

            // Act & Assert - Invalid user
            assertFalse(adapter.isActiveUser(invalidUserId), "Invalid user should not be active");
            assertFalse(adapter.findById(invalidUserId).isPresent(), "Invalid user should not be found");
        }
    }
}
