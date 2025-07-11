package com.vclipper.processing.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebConfigurationTest {

    private WebConfiguration webConfiguration;

    @Mock
    private CorsRegistry corsRegistry;

    @Mock
    private CorsRegistration corsRegistration;

    @BeforeEach
    void setUp() {
        webConfiguration = new WebConfiguration();
    }

    @Test
    void shouldConfigureCorsWithValidAllowedOrigins() {
        // Arrange
        String allowedOrigins = "http://localhost:3000, http://localhost:4200";
        ReflectionTestUtils.setField(webConfiguration, "allowedOriginsString", allowedOrigins);

        // Set up mock chain only in this test where it's used
        when(corsRegistry.addMapping("/api/**")).thenReturn(corsRegistration);
        when(corsRegistry.addMapping("/actuator/**")).thenReturn(corsRegistration);
        when(corsRegistration.allowedOrigins(any(String[].class))).thenReturn(corsRegistration);
        when(corsRegistration.allowedMethods(any(String[].class))).thenReturn(corsRegistration);
        when(corsRegistration.allowedHeaders(any(String[].class))).thenReturn(corsRegistration);
        when(corsRegistration.allowCredentials(anyBoolean())).thenReturn(corsRegistration);
        when(corsRegistration.maxAge(anyLong())).thenReturn(corsRegistration);

        // Act
        webConfiguration.addCorsMappings(corsRegistry);

        // Assert
        verify(corsRegistry).addMapping("/api/**");
        verify(corsRegistry).addMapping("/actuator/**");

        // Verify the full chain of calls
        verify(corsRegistration, times(2)).allowedOrigins(any(String[].class));
        verify(corsRegistration, times(2)).allowedMethods(any(String[].class));
        verify(corsRegistration, times(2)).allowedHeaders(any(String[].class));
        verify(corsRegistration, times(1)).allowCredentials(anyBoolean());
        verify(corsRegistration, times(2)).maxAge(anyLong());
    }

    @Test
    void shouldParseAllowedOriginsCorrectly() {
        // Arrange
        String allowedOrigins = "http://localhost:3000, http://localhost:4200";
        ReflectionTestUtils.setField(webConfiguration, "allowedOriginsString", allowedOrigins);

        // Act & Assert - using reflection to test private method
        String[] result = (String[]) ReflectionTestUtils.invokeMethod(webConfiguration, "parseAllowedOrigins");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("http://localhost:3000", result[0]);
        assertEquals("http://localhost:4200", result[1]);
    }

    @Test
    void shouldThrowExceptionWhenAllowedOriginsIsEmpty() {
        // Arrange
        ReflectionTestUtils.setField(webConfiguration, "allowedOriginsString", "");

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> webConfiguration.addCorsMappings(corsRegistry)
        );

        assertEquals("CORS_ALLOWED_ORIGINS environment variable is required but not configured", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAllowedOriginsIsNull() {
        // Arrange
        ReflectionTestUtils.setField(webConfiguration, "allowedOriginsString", null);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> webConfiguration.addCorsMappings(corsRegistry)
        );

        assertEquals("CORS_ALLOWED_ORIGINS environment variable is required but not configured", exception.getMessage());
    }
}
