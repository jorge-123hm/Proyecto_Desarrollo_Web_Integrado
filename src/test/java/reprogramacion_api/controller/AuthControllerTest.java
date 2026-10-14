package reprogramacion_api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import reprogramacion_api.dto.request.LoginRequest;
import reprogramacion_api.dto.response.LoginResponse;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.security.JwtService;
import reprogramacion_api.support.Datos;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthController controller;

    @Test
    void login_credencialesValidas_devuelveTokenYDatosDelUsuario() {
        Usuario u = Datos.usuario(2L, Rol.DOCENTE, "docente@utp.edu.pe");
        when(usuarioRepository.findByCorreo("docente@utp.edu.pe")).thenReturn(Optional.of(u));
        when(jwtService.generateToken("docente@utp.edu.pe", Rol.DOCENTE)).thenReturn("TOKEN");

        ResponseEntity<LoginResponse> res = controller.login(new LoginRequest("docente@utp.edu.pe", "123456"));

        assertEquals(200, res.getStatusCode().value());
        LoginResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("TOKEN", body.token());
        assertEquals("Bearer", body.tipo());
        assertEquals(2L, body.usuarioId().longValue());
        assertEquals(Rol.DOCENTE, body.rol());
        verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @Test
    void login_credencialesInvalidas_propagaLaExcepcionYNoGeneraToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("mal"));

        assertThrows(BadCredentialsException.class,
                () -> controller.login(new LoginRequest("docente@utp.edu.pe", "incorrecta")));
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void login_usuarioNoEncontradoTrasAutenticar_lanzaNoSuchElement() {
        when(usuarioRepository.findByCorreo("fantasma@utp.edu.pe")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> controller.login(new LoginRequest("fantasma@utp.edu.pe", "123456")));
    }
}
