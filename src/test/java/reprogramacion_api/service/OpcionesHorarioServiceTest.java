package reprogramacion_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.BloqueNoDisponible;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.BloqueNoDisponibleRepository;
import reprogramacion_api.repository.ClaseRepository;
import reprogramacion_api.repository.MatriculaRepository;
import reprogramacion_api.service.algoritmo.CalculadorDisponibilidad;
import reprogramacion_api.service.algoritmo.OpcionHorario;
import reprogramacion_api.support.Datos;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Usa el CalculadorDisponibilidad real y simula solo los repositorios. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpcionesHorarioServiceTest {

    private static final String DOCENTE = "docente@utp.edu.pe";

    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private MatriculaRepository matriculaRepo;
    @Mock
    private BloqueNoDisponibleRepository bloqueRepo;

    private OpcionesHorarioService service;
    private Usuario docente;
    private Usuario alumno3;
    private Usuario alumno4;
    private Curso curso;

    @BeforeEach
    void setUp() {
        service = new OpcionesHorarioService(claseRepo, matriculaRepo, bloqueRepo, new CalculadorDisponibilidad());

        docente = Datos.usuario(2L, Rol.DOCENTE, DOCENTE);
        alumno3 = Datos.usuario(3L, Rol.ALUMNO, "a3@utp.edu.pe");
        alumno4 = Datos.usuario(4L, Rol.ALUMNO, "a4@utp.edu.pe");
        curso = Datos.curso(10L, docente);
        Clase clase = Datos.clase(100L, curso); // dura 2 horas (08:00-10:00)

        when(claseRepo.findById(100L)).thenReturn(Optional.of(clase));
        when(matriculaRepo.findByCursoId(10L)).thenReturn(List.of(
                Datos.matricula(alumno3, curso), Datos.matricula(alumno4, curso)));
        when(bloqueRepo.findByUsuarioIdIn(any())).thenReturn(List.of());
    }

    /** Bloques de lunes a sábado de 07:00 a 22:00 para el usuario dado. */
    private static List<BloqueNoDisponible> todoElDia(Usuario u) {
        List<BloqueNoDisponible> lista = new ArrayList<>();
        for (DayOfWeek d = DayOfWeek.MONDAY; d != DayOfWeek.SUNDAY; d = d.plus(1)) {
            lista.add(Datos.bloque(u, d, 7, 22));
        }
        return lista;
    }

    // ---------- calcular ----------

    @Test
    void calcular_sinParametros_usaRangoYLimitePorDefecto() {
        List<OpcionHorario> res = service.calcular(DOCENTE, 100L, null, null, null);

        assertEquals(10, res.size());
        assertTrue(res.stream().allMatch(o -> o.porcentaje() == 100.0));
        assertTrue(res.stream().allMatch(OpcionHorario::docenteDisponible));
    }

    @Test
    void calcular_limiteCero_seAjustaAUno() {
        List<OpcionHorario> res = service.calcular(DOCENTE, 100L, null, null, 0);

        assertEquals(1, res.size());
    }

    @Test
    void calcular_limiteMuyAlto_seTopeaEn50() {
        List<OpcionHorario> res = service.calcular(DOCENTE, 100L, null, null, 999);

        assertEquals(50, res.size());
    }

    @Test
    void calcular_rangoDe60Dias_esValido() {
        LocalDate desde = Datos.futuro();

        List<OpcionHorario> res = service.calcular(DOCENTE, 100L, desde, desde.plusDays(60), 5);

        assertEquals(5, res.size());
    }

    @Test
    void calcular_rangoMayorA60Dias_lanzaReglaNegocio() {
        LocalDate desde = Datos.futuro();

        assertThrows(ReglaNegocioException.class,
                () -> service.calcular(DOCENTE, 100L, desde, desde.plusDays(61), 5));
    }

    @Test
    void calcular_claseInexistente_lanzaNoEncontrado() {
        when(claseRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.calcular(DOCENTE, 999L, null, null, null));
    }

    @Test
    void calcular_claseDeOtroDocente_lanzaAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.calcular("otro@utp.edu.pe", 100L, null, null, null));
    }

    @Test
    void calcular_cursoSinAlumnos_lanzaReglaNegocio() {
        when(matriculaRepo.findByCursoId(10L)).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> service.calcular(DOCENTE, 100L, null, null, null));
    }

    @Test
    void calcular_alumnoSiempreOcupado_bajaElPorcentajeA50() {
        when(bloqueRepo.findByUsuarioIdIn(any())).thenReturn(todoElDia(alumno3));

        List<OpcionHorario> res = service.calcular(DOCENTE, 100L, null, null, 5);

        assertEquals(5, res.size());
        for (OpcionHorario o : res) {
            assertEquals(50.0, o.porcentaje());
            assertEquals(List.of(3L), o.alumnosConConflicto());
        }
    }

    @Test
    void calcular_docenteSiempreOcupado_noHayOpciones() {
        when(bloqueRepo.findByUsuarioIdIn(any())).thenReturn(todoElDia(docente));

        List<OpcionHorario> res = service.calcular(DOCENTE, 100L, null, null, 10);

        assertTrue(res.isEmpty());
    }

    // ---------- evaluar ----------

    @Test
    void evaluar_franjaLibre_devuelve100PorCiento() {
        OpcionHorario o = service.evaluar(DOCENTE, 100L, Datos.futuro(),
                LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertTrue(o.docenteDisponible());
        assertEquals(100.0, o.porcentaje());
        assertEquals(2, o.totalAlumnos());
        assertTrue(o.alumnosConConflicto().isEmpty());
    }

    @Test
    void evaluar_alumnoConCruce_loReportaComoConflicto() {
        LocalDate fecha = Datos.futuro();
        when(bloqueRepo.findByUsuarioIdIn(any())).thenReturn(List.of(
                Datos.bloque(alumno3, fecha.getDayOfWeek(), 14, 15)));

        OpcionHorario o = service.evaluar(DOCENTE, 100L, fecha, LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertEquals(List.of(3L), o.alumnosConConflicto());
        assertEquals(50.0, o.porcentaje());
        assertTrue(o.docenteDisponible());
    }

    @Test
    void evaluar_docenteConBloque_noEstaDisponible() {
        LocalDate fecha = Datos.futuro();
        when(bloqueRepo.findByUsuarioIdIn(any())).thenReturn(List.of(
                Datos.bloque(docente, fecha.getDayOfWeek(), 13, 17)));

        OpcionHorario o = service.evaluar(DOCENTE, 100L, fecha, LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertFalse(o.docenteDisponible());
    }

    @Test
    void evaluar_horaInicioNoAnteriorAFin_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> service.evaluar(DOCENTE, 100L, Datos.futuro(),
                LocalTime.of(16, 0), LocalTime.of(14, 0)));
    }

    @Test
    void evaluar_fechaDeHoy_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> service.evaluar(DOCENTE, 100L, Datos.hoy(),
                LocalTime.of(14, 0), LocalTime.of(16, 0)));
    }

    @Test
    void evaluar_fechaPasada_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> service.evaluar(DOCENTE, 100L, Datos.pasado(),
                LocalTime.of(14, 0), LocalTime.of(16, 0)));
    }

    @Test
    void evaluar_cursoSinAlumnos_lanzaReglaNegocio() {
        when(matriculaRepo.findByCursoId(10L)).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class, () -> service.evaluar(DOCENTE, 100L, Datos.futuro(),
                LocalTime.of(14, 0), LocalTime.of(16, 0)));
    }

    @Test
    void evaluar_claseDeOtroDocente_lanzaAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> service.evaluar("otro@utp.edu.pe", 100L,
                Datos.futuro(), LocalTime.of(14, 0), LocalTime.of(16, 0)));
    }
}
