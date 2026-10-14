package reprogramacion_api.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterTest {

    private static final String CORREO = "docente@utp.edu.pe";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private FilterChain chain;

    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        user = new User(CORREO, "hash", List.of(new SimpleGrantedAuthority("ROLE_DOCENTE")));
        when(userDetailsService.loadUserByUsername(CORREO)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinCabeceraAuthorization_continuaSinAutenticar() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void cabeceraQueNoEsBearer_continuaSinAutenticar() throws Exception {
        request.addHeader("Authorization", "Basic abc123");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void tokenValido_dejaAlUsuarioAutenticado() throws Exception {
        request.addHeader("Authorization", "Bearer abc");
        when(jwtService.extractUsername("abc")).thenReturn(CORREO);
        when(jwtService.isTokenValid("abc", user)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(CORREO, auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenQueNoCoincideConElUsuario_noAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer abc");
        when(jwtService.extractUsername("abc")).thenReturn(CORREO);
        when(jwtService.isTokenValid("abc", user)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenInvalidoOExpirado_continuaSinAutenticar() throws Exception {
        request.addHeader("Authorization", "Bearer malo");
        when(jwtService.extractUsername("malo")).thenThrow(new JwtException("expirado"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenConArgumentoIlegal_continuaSinAutenticar() throws Exception {
        request.addHeader("Authorization", "Bearer raro");
        when(jwtService.extractUsername("raro")).thenThrow(new IllegalArgumentException("vacío"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void usuarioDelTokenYaNoExiste_continuaSinAutenticar() throws Exception {
        request.addHeader("Authorization", "Bearer abc");
        when(jwtService.extractUsername("abc")).thenReturn("borrado@utp.edu.pe");
        when(userDetailsService.loadUserByUsername("borrado@utp.edu.pe"))
                .thenThrow(new UsernameNotFoundException("no existe"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenSinSubject_noIntentaCargarUsuario() throws Exception {
        request.addHeader("Authorization", "Bearer sinsujeto");
        when(jwtService.extractUsername("sinsujeto")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void contextoYaAutenticado_noVuelveAAutenticar() throws Exception {
        Authentication previa = new UsernamePasswordAuthenticationToken("previo", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(previa);
        request.addHeader("Authorization", "Bearer abc");
        when(jwtService.extractUsername("abc")).thenReturn(CORREO);

        filter.doFilter(request, response, chain);

        assertSame(previa, SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain).doFilter(request, response);
    }
}
