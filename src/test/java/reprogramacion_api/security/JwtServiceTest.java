package reprogramacion_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import reprogramacion_api.model.enums.Rol;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "PROYECTODESARROLLOWEBINTEGRADO_111222333444555666777888999";
    private static final String CORREO = "docente@utp.edu.pe";

    private final JwtService service = new JwtService(SECRET, 3_600_000L);

    private static UserDetails userDetails(String correo) {
        return new User(correo, "hash", List.of(new SimpleGrantedAuthority("ROLE_DOCENTE")));
    }

    @Test
    void generateToken_yExtractUsername_devuelvenElMismoCorreo() {
        String token = service.generateToken(CORREO, Rol.DOCENTE);

        assertEquals(CORREO, service.extractUsername(token));
    }

    @Test
    void generateToken_incluyeElRolComoClaim() {
        String token = service.generateToken(CORREO, Rol.COORDINACION);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertEquals("COORDINACION", claims.get("rol", String.class));
        assertNotNull(claims.getIssuedAt());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void isTokenValid_conElMismoUsuario_esVerdadero() {
        String token = service.generateToken(CORREO, Rol.DOCENTE);

        assertTrue(service.isTokenValid(token, userDetails(CORREO)));
    }

    @Test
    void isTokenValid_conOtroUsuario_esFalso() {
        String token = service.generateToken(CORREO, Rol.DOCENTE);

        assertFalse(service.isTokenValid(token, userDetails("otro@utp.edu.pe")));
    }

    @Test
    void extractUsername_conTokenExpirado_lanzaJwtException() {
        JwtService expirado = new JwtService(SECRET, -1_000L);
        String token = expirado.generateToken(CORREO, Rol.DOCENTE);

        assertThrows(JwtException.class, () -> service.extractUsername(token));
    }

    @Test
    void extractUsername_conTokenMalformado_lanzaJwtException() {
        assertThrows(JwtException.class, () -> service.extractUsername("esto.no.es.un.token"));
    }

    @Test
    void extractUsername_conFirmaDeOtraClave_lanzaJwtException() {
        JwtService otraClave = new JwtService("OTRA_CLAVE_SECRETA_DE_AL_MENOS_32_BYTES_123456", 3_600_000L);
        String token = otraClave.generateToken(CORREO, Rol.DOCENTE);

        assertThrows(JwtException.class, () -> service.extractUsername(token));
    }
}
