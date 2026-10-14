package reprogramacion_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reprogramacion_api.dto.response.NotificacionResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.model.Notificacion;
import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.NotificacionRepository;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.support.Datos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacionServiceTest {

    private static final String CORREO = "alumno@utp.edu.pe";

    @Mock
    private NotificacionRepository repo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @InjectMocks
    private NotificacionService service;
    @Captor
    private ArgumentCaptor<List<Notificacion>> captor;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Datos.usuario(3L, Rol.ALUMNO, CORREO);
        when(usuarioRepo.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
    }

    @Test
    void notificarVarios_creaUnaNotificacionPorDestinatario() {
        Usuario u1 = Datos.usuario(1L, Rol.ALUMNO, "a1@utp.edu.pe");
        Usuario u2 = Datos.usuario(2L, Rol.ALUMNO, "a2@utp.edu.pe");
        SolicitudReprogramacion solicitud = SolicitudReprogramacion.builder().id(5L).build();

        service.notificarVarios(List.of(u1, u2), solicitud, "Hola");

        verify(repo).saveAll(captor.capture());
        List<Notificacion> guardadas = captor.getValue();
        assertEquals(2, guardadas.size());
        assertSame(u1, guardadas.get(0).getUsuario());
        assertSame(u2, guardadas.get(1).getUsuario());
        assertSame(solicitud, guardadas.get(0).getSolicitud());
        assertEquals("Hola", guardadas.get(0).getMensaje());
        assertFalse(guardadas.get(0).isLeida());
    }

    @Test
    void notificarVarios_sinDestinatarios_guardaListaVacia() {
        service.notificarVarios(List.of(), null, "Nadie");

        verify(repo).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void listar_devuelveLasNotificacionesDelUsuario() {
        Notificacion n = Notificacion.builder().id(1L).usuario(usuario).mensaje("m").build();
        when(repo.findByUsuarioIdOrderByFechaDesc(3L)).thenReturn(List.of(n));

        List<NotificacionResponse> res = service.listar(CORREO);

        assertEquals(1, res.size());
        assertEquals("m", res.get(0).mensaje());
    }

    @Test
    void listar_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepo.findByCorreo("nadie@utp.edu.pe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.listar("nadie@utp.edu.pe"));
    }

    @Test
    void contarNoLeidas_devuelveMapaConLaCantidad() {
        when(repo.countByUsuarioIdAndLeidaFalse(3L)).thenReturn(4L);

        Map<String, Long> res = service.contarNoLeidas(CORREO);

        assertEquals(Long.valueOf(4L), res.get("cantidad"));
    }

    @Test
    void marcarLeida_ok_cambiaElEstado() {
        Notificacion n = Notificacion.builder().id(8L).usuario(usuario).mensaje("m").build();
        when(repo.findByIdAndUsuarioId(8L, 3L)).thenReturn(Optional.of(n));
        when(repo.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificacionResponse res = service.marcarLeida(CORREO, 8L);

        assertTrue(n.isLeida());
        assertTrue(res.leida());
    }

    @Test
    void marcarLeida_notificacionAjenaOInexistente_lanzaNoEncontrado() {
        when(repo.findByIdAndUsuarioId(8L, 3L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.marcarLeida(CORREO, 8L));
    }
}
