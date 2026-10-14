package reprogramacion_api.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reprogramacion_api.dto.response.BloqueResponse;
import reprogramacion_api.dto.response.ClaseResponse;
import reprogramacion_api.dto.response.NotificacionResponse;
import reprogramacion_api.dto.response.PropuestaResponse;
import reprogramacion_api.dto.response.SolicitudResponse;
import reprogramacion_api.dto.response.UsuarioResponse;
import reprogramacion_api.model.BloqueNoDisponible;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.Notificacion;
import reprogramacion_api.model.PropuestaReprogramacion;
import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.EstadoSolicitud;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.support.Datos;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica los métodos from(...) de los DTO y los valores por defecto de las entidades. */
class DtoYModelosTest {

    private Usuario docente;
    private Usuario coord;
    private Curso curso;
    private Clase clase;
    private PropuestaReprogramacion propuesta;

    @BeforeEach
    void setUp() {
        docente = Datos.usuario(2L, Rol.DOCENTE, "docente@utp.edu.pe");
        coord = Datos.usuario(1L, Rol.COORDINACION, "coord@utp.edu.pe");
        curso = Datos.curso(10L, docente);
        clase = Datos.clase(100L, curso);
        propuesta = Datos.propuesta(500L, clase, Datos.futuro());
    }

    @Test
    void usuarioResponse_copiaLosCampos() {
        UsuarioResponse r = UsuarioResponse.from(docente);

        assertEquals(2L, r.id().longValue());
        assertEquals("U2", r.cui());
        assertEquals("Nombre2", r.nombres());
        assertEquals("Apellido2", r.apellidos());
        assertEquals("docente@utp.edu.pe", r.correo());
        assertEquals(Rol.DOCENTE, r.rol());
        assertTrue(r.activo());
    }

    @Test
    void bloqueResponse_copiaLosCampos() {
        BloqueNoDisponible b = Datos.bloque(docente, DayOfWeek.FRIDAY, 9, 11);

        BloqueResponse r = BloqueResponse.from(b);

        assertEquals(DayOfWeek.FRIDAY, r.diaSemana());
        assertEquals(9, r.horaInicio().getHour());
        assertEquals(11, r.horaFin().getHour());
        assertEquals("Motivo", r.motivo());
    }

    @Test
    void claseResponse_incluyeDatosDelCurso() {
        ClaseResponse r = ClaseResponse.from(clase);

        assertEquals(100L, r.id().longValue());
        assertEquals(10L, r.cursoId().longValue());
        assertEquals("Desarrollo Web", r.curso());
        assertEquals("A", r.seccion());
        assertEquals("Spring Boot", r.tema());
    }

    @Test
    void notificacionResponse_sinSolicitud_dejaSolicitudIdNulo() {
        Notificacion n = Notificacion.builder().id(1L).usuario(docente).mensaje("hola").build();

        NotificacionResponse r = NotificacionResponse.from(n);

        assertNull(r.solicitudId());
        assertEquals("hola", r.mensaje());
        assertFalse(r.leida());
        assertNotNull(r.fecha());
    }

    @Test
    void notificacionResponse_conSolicitud_incluyeElId() {
        SolicitudReprogramacion s = Datos.solicitud(900L, propuesta, docente, EstadoSolicitud.PENDIENTE);
        Notificacion n = Notificacion.builder().id(1L).usuario(docente).solicitud(s).mensaje("hola").build();

        NotificacionResponse r = NotificacionResponse.from(n);

        assertEquals(900L, r.solicitudId().longValue());
    }

    @Test
    void propuestaResponse_combinaClaseYPropuesta() {
        PropuestaResponse r = PropuestaResponse.from(propuesta);

        assertEquals(500L, r.id().longValue());
        assertEquals(100L, r.claseId().longValue());
        assertEquals("Desarrollo Web", r.curso());
        assertEquals(clase.getFechaOriginal(), r.fechaOriginal());
        assertEquals(propuesta.getFechaPropuesta(), r.fechaPropuesta());
        assertEquals(75.0, r.porcentajeCoincidencia());
        assertEquals(1, r.alumnosConConflicto());
        assertFalse(r.manual());
    }

    @Test
    void solicitudResponse_sinResolver_dejaResueltoPorNulo() {
        SolicitudReprogramacion s = Datos.solicitud(900L, propuesta, docente, EstadoSolicitud.PENDIENTE);

        SolicitudResponse r = SolicitudResponse.from(s);

        assertEquals(900L, r.id().longValue());
        assertEquals("Nombre2 Apellido2", r.docente());
        assertEquals(EstadoSolicitud.PENDIENTE, r.estado());
        assertNull(r.resueltoPor());
        assertNull(r.fechaResolucion());
    }

    @Test
    void solicitudResponse_resuelta_incluyeAQuienResolvio() {
        SolicitudReprogramacion s = Datos.solicitud(900L, propuesta, docente, EstadoSolicitud.APROBADA);
        s.setResueltoPor(coord);
        s.setFechaResolucion(LocalDateTime.now());
        s.setObservacion("ok");

        SolicitudResponse r = SolicitudResponse.from(s);

        assertEquals("Nombre1 Apellido1", r.resueltoPor());
        assertEquals("ok", r.observacion());
        assertNotNull(r.fechaResolucion());
    }

    // ---------- valores por defecto de las entidades ----------

    @Test
    void entidades_tienenValoresPorDefectoCorrectos() {
        assertEquals(EstadoSolicitud.PENDIENTE, SolicitudReprogramacion.builder().build().getEstado());
        assertNotNull(SolicitudReprogramacion.builder().build().getFechaSolicitud());
        assertFalse(Notificacion.builder().build().isLeida());
        assertFalse(PropuestaReprogramacion.builder().build().isManual());
        assertTrue(Usuario.builder().build().isActivo());
    }

    @Test
    void enums_exponenSusValores() {
        assertEquals(3, Rol.values().length);
        assertEquals(3, EstadoSolicitud.values().length);
        assertEquals(Rol.ALUMNO, Rol.valueOf("ALUMNO"));
        assertEquals(EstadoSolicitud.RECHAZADA, EstadoSolicitud.valueOf("RECHAZADA"));
    }
}
