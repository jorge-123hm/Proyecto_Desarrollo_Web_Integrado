package reprogramacion_api.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.support.Datos;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_usuarioActivo_devuelveDetallesConSuRol() {
        Usuario u = Datos.usuario(2L, Rol.DOCENTE, "docente@utp.edu.pe");
        when(usuarioRepository.findByCorreo("docente@utp.edu.pe")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("docente@utp.edu.pe");

        assertEquals("docente@utp.edu.pe", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCENTE")));
    }

    @Test
    void loadUserByUsername_usuarioInactivo_quedaDeshabilitado() {
        Usuario u = Datos.usuario(3L, Rol.ALUMNO, "alumno@utp.edu.pe");
        u.setActivo(false);
        when(usuarioRepository.findByCorreo("alumno@utp.edu.pe")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("alumno@utp.edu.pe");

        assertFalse(details.isEnabled());
    }

    @Test
    void loadUserByUsername_correoInexistente_lanzaUsernameNotFound() {
        when(usuarioRepository.findByCorreo("nadie@utp.edu.pe")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nadie@utp.edu.pe"));
    }
}
