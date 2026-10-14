package reprogramacion_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import reprogramacion_api.dto.response.SolicitudResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.PropuestaReprogramacion;
import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.EstadoSolicitud;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.MatriculaRepository;
import reprogramacion_api.repository.PropuestaReprogramacionRepository;
import reprogramacion_api.repository.SolicitudReprogramacionRepository;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.support.Datos;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudServiceTest {

    private static final String DOCENTE = "docente@utp.edu.pe";
    private static final String COORD = "coord@utp.edu.pe";

    @Mock
    private SolicitudReprogramacionRepository solicitudRepo;
    @Mock
    private PropuestaReprogramacionRepository propuestaRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private MatriculaRepository matriculaRepo;
    @Mock
    private NotificacionService notificacionService;
    @InjectMocks
    private SolicitudService service;

    private Usuario docente;
    private Usuario coord;
    private Usuario alumno3;
    private Usuario alumno4;
    private Curso curso;
    private Clase clase;
    private PropuestaReprogramacion propuesta;

    @BeforeEach
    void setUp() {
        docente = Datos.usuario(2L, Rol.DOCENTE, DOCENTE);
        coord = Datos.usuario(1L, Rol.COORDINACION, COORD);
        alumno3 = Datos.usuario(3L, Rol.ALUMNO, "a3@utp.edu.pe");
        alumno4 = Datos.usuario(4L, Rol.ALUMNO, "a4@utp.edu.pe");
        curso = Datos.curso(10L, docente);
        clase = Datos.clase(100L, curso);
        propuesta = Datos.propuesta(500L, clase, Datos.futuro());

        when(usuarioRepo.findByCorreo(DOCENTE)).thenReturn(Optional.of(docente));
        when(usuarioRepo.findByCorreo(COORD)).thenReturn(Optional.of(coord));
        when(propuestaRepo.findById(500L)).thenReturn(Optional.of(propuesta));
        when(solicitudRepo.save(any(SolicitudReprogramacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepo.findByRol(Rol.COORDINACION)).thenReturn(List.of(coord));
        when(matriculaRepo.findByCursoId(10L)).thenReturn(List.of(
                Datos.matricula(alumno3, curso), Datos.matricula(alumno4, curso)));
    }

    private SolicitudReprogramacion pendiente() {
        SolicitudReprogramacion s = Datos.solicitud(900L, propuesta, docente, EstadoSolicitud.PENDIENTE);
        when(solicitudRepo.findById(900L)).thenReturn(Optional.of(s));
        return s;
    }

    // ---------- enviar ----------

    @Test
    void enviar_ok_creaSolicitudPendienteYAvisaACoordinacion() {
        SolicitudResponse res = service.enviar(DOCENTE, 500L);

        assertEquals(EstadoSolicitud.PENDIENTE, res.estado());
        assertEquals(500L, res.propuestaId().longValue());
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificarVarios(eq(List.of(coord)),
                any(SolicitudReprogramacion.class), mensaje.capture());
        assertTrue(mensaje.getValue().contains("Nueva solicitud de Nombre2 Apellido2"));
    }

    @Test
    void enviar_docenteInexistente_lanzaNoEncontrado() {
        when(usuarioRepo.findByCorreo("nadie@utp.edu.pe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.enviar("nadie@utp.edu.pe", 500L));
    }

    @Test
    void enviar_propuestaInexistente_lanzaNoEncontrado() {
        when(propuestaRepo.findById(500L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.enviar(DOCENTE, 500L));
    }

    @Test
    void enviar_propuestaDeOtroDocente_lanzaAccessDenied() {
        Usuario otro = Datos.usuario(77L, Rol.DOCENTE, "otro@utp.edu.pe");
        when(usuarioRepo.findByCorreo("otro@utp.edu.pe")).thenReturn(Optional.of(otro));

        assertThrows(AccessDeniedException.class, () -> service.enviar("otro@utp.edu.pe", 500L));
        verify(solicitudRepo, never()).save(any());
    }

    @Test
    void enviar_propuestaYaEnviada_lanzaReglaNegocio() {
        when(solicitudRepo.existsByPropuestaId(500L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.enviar(DOCENTE, 500L));
        verify(solicitudRepo, never()).save(any());
    }

    @Test
    void enviar_yaHayPendienteParaLaClase_lanzaReglaNegocio() {
        when(solicitudRepo.existsByPropuestaClaseIdAndEstado(100L, EstadoSolicitud.PENDIENTE)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.enviar(DOCENTE, 500L));
    }

    @Test
    void enviar_fechaPropuestaVencida_lanzaReglaNegocio() {
        propuesta.setFechaPropuesta(Datos.pasado());

        assertThrows(ReglaNegocioException.class, () -> service.enviar(DOCENTE, 500L));
    }

    @Test
    void enviar_fechaPropuestaDeHoy_lanzaReglaNegocio() {
        propuesta.setFechaPropuesta(Datos.hoy());

        assertThrows(ReglaNegocioException.class, () -> service.enviar(DOCENTE, 500L));
    }

    // ---------- resolver ----------

    @Test
    void resolver_decisionPendiente_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> service.resolver(COORD, 900L, EstadoSolicitud.PENDIENTE, "x"));
    }

    @Test
    void resolver_rechazoSinObservacion_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> service.resolver(COORD, 900L, EstadoSolicitud.RECHAZADA, null));
    }

    @Test
    void resolver_rechazoConObservacionEnBlanco_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> service.resolver(COORD, 900L, EstadoSolicitud.RECHAZADA, "   "));
    }

    @Test
    void resolver_coordinadorInexistente_lanzaNoEncontrado() {
        when(usuarioRepo.findByCorreo("fantasma@utp.edu.pe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.resolver("fantasma@utp.edu.pe", 900L, EstadoSolicitud.APROBADA, null));
    }

    @Test
    void resolver_solicitudInexistente_lanzaNoEncontrado() {
        when(solicitudRepo.findById(900L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.resolver(COORD, 900L, EstadoSolicitud.APROBADA, null));
    }

    @Test
    void resolver_solicitudYaResuelta_lanzaReglaNegocio() {
        SolicitudReprogramacion s = pendiente();
        s.setEstado(EstadoSolicitud.APROBADA);

        assertThrows(ReglaNegocioException.class,
                () -> service.resolver(COORD, 900L, EstadoSolicitud.RECHAZADA, "motivo"));
    }

    @Test
    void resolver_aprobarConFechaVencida_lanzaReglaNegocio() {
        pendiente();
        propuesta.setFechaPropuesta(Datos.pasado());

        assertThrows(ReglaNegocioException.class,
                () -> service.resolver(COORD, 900L, EstadoSolicitud.APROBADA, null));
        verify(solicitudRepo, never()).save(any());
    }

    @Test
    void resolver_aprobar_actualizaEstadoYNotificaADocenteYAlumnos() {
        SolicitudReprogramacion s = pendiente();

        SolicitudResponse res = service.resolver(COORD, 900L, EstadoSolicitud.APROBADA, "  Conforme  ");

        assertEquals(EstadoSolicitud.APROBADA, s.getEstado());
        assertEquals("Conforme", s.getObservacion());
        assertNotNull(s.getFechaResolucion());
        assertSame(coord, s.getResueltoPor());
        assertEquals(EstadoSolicitud.APROBADA, res.estado());
        assertEquals("Nombre1 Apellido1", res.resueltoPor());
        verify(notificacionService).notificarVarios(eq(List.of(docente)), eq(s), contains("APROBADA"));
        verify(notificacionService).notificarVarios(eq(List.of(alumno3, alumno4)), eq(s),
                contains("fue reprogramada"));
    }

    @Test
    void resolver_aprobarSinObservacion_guardaObservacionNula() {
        SolicitudReprogramacion s = pendiente();

        service.resolver(COORD, 900L, EstadoSolicitud.APROBADA, "   ");

        assertNull(s.getObservacion());
    }

    @Test
    void resolver_rechazar_notificaSoloAlDocenteConElMotivo() {
        SolicitudReprogramacion s = pendiente();

        SolicitudResponse res = service.resolver(COORD, 900L, EstadoSolicitud.RECHAZADA, "No hay aula");

        assertEquals(EstadoSolicitud.RECHAZADA, res.estado());
        assertEquals("No hay aula", res.observacion());
        verify(notificacionService, times(1)).notificarVarios(anyCollection(), any(), anyString());
        verify(notificacionService).notificarVarios(eq(List.of(docente)), eq(s), contains("Motivo: No hay aula"));
        verify(matriculaRepo, never()).findByCursoId(any());
    }

    @Test
    void resolver_rechazarConFechaVencida_esPermitido() {
        pendiente();
        propuesta.setFechaPropuesta(Datos.pasado());

        SolicitudResponse res = service.resolver(COORD, 900L, EstadoSolicitud.RECHAZADA, "Fecha vencida");

        assertEquals(EstadoSolicitud.RECHAZADA, res.estado());
    }

    // ---------- historial / obtener ----------

    @Test
    void historial_coordinacion_noFiltraPorDocente() {
        SolicitudReprogramacion s = Datos.solicitud(900L, propuesta, docente, EstadoSolicitud.PENDIENTE);
        when(solicitudRepo.buscar(null, 10L, EstadoSolicitud.PENDIENTE)).thenReturn(List.of(s));

        List<SolicitudResponse> res = service.historial(COORD, true, EstadoSolicitud.PENDIENTE, 10L);

        assertEquals(1, res.size());
        verify(usuarioRepo, never()).findByCorreo(COORD);
    }

    @Test
    void historial_docente_filtraPorSuId() {
        SolicitudReprogramacion s = Datos.solicitud(900L, propuesta, docente, EstadoSolicitud.APROBADA);
        when(solicitudRepo.buscar(2L, null, null)).thenReturn(List.of(s));

        List<SolicitudResponse> res = service.historial(DOCENTE, false, null, null);

        assertEquals(1, res.size());
        assertEquals(EstadoSolicitud.APROBADA, res.get(0).estado());
    }

    @Test
    void historial_docenteInexistente_lanzaNoEncontrado() {
        when(usuarioRepo.findByCorreo("nadie@utp.edu.pe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.historial("nadie@utp.edu.pe", false, null, null));
    }

    @Test
    void obtener_coordinacion_puedeVerCualquierSolicitud() {
        pendiente();

        SolicitudResponse res = service.obtener(COORD, true, 900L);

        assertEquals(900L, res.id().longValue());
    }

    @Test
    void obtener_docenteDuenio_puedeVerSuSolicitud() {
        pendiente();

        SolicitudResponse res = service.obtener(DOCENTE, false, 900L);

        assertEquals("Nombre2 Apellido2", res.docente());
    }

    @Test
    void obtener_docenteAjeno_lanzaAccessDenied() {
        pendiente();

        assertThrows(AccessDeniedException.class, () -> service.obtener("otro@utp.edu.pe", false, 900L));
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(solicitudRepo.findById(900L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(COORD, true, 900L));
    }
}
