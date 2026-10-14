package reprogramacion_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import reprogramacion_api.security.JwtAuthFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class));

    @Test
    void passwordEncoder_usaBCrypt() {
        PasswordEncoder encoder = config.passwordEncoder();

        String hash = encoder.encode("123456");

        assertNotEquals("123456", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("123456", hash));
        assertFalse(encoder.matches("otra", hash));
    }

    @Test
    void authenticationManager_devuelveElDeLaConfiguracion() throws Exception {
        AuthenticationConfiguration cfg = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(cfg.getAuthenticationManager()).thenReturn(manager);

        assertSame(manager, config.authenticationManager(cfg));
    }

    @Test
    void cors_permiteSoloAngularEnLocalhost4200() {
        CorsConfigurationSource source = config.corsConfigurationSource();

        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/cursos"));

        assertNotNull(cors);
        assertEquals(List.of("http://localhost:4200"), cors.getAllowedOrigins());
        assertTrue(cors.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "DELETE")));
        assertEquals(List.of("Authorization", "Content-Type"), cors.getAllowedHeaders());
        assertEquals(3600L, cors.getMaxAge().longValue());
    }
}
