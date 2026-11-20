package reprogramacion_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reprogramacion_api.dto.request.BloqueRequest;
import reprogramacion_api.dto.response.BloqueResponse;
import reprogramacion_api.dto.response.DisponibilidadResumenResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.BloqueNoDisponible;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.BloqueNoDisponibleRepository;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.support.Datos;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisponibilidadServiceTest {

    private static final String CORREO = "alumno@utp.edu.pe";

    @Mock
    private BloqueNoDisponibleRepository bloqueRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @InjectMocks
    private DisponibilidadService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Datos.usuario(3L, Rol.ALUMNO, CORREO);
        when(usuarioRepo.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
    }

    private static BloqueRequest bloque(DayOfWeek dia, int desde, int hasta) {
        return new BloqueRequest(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), "Motivo");
    }

    // ---------- listar ----------

    @Test
    void listar_ordenaPorDiaYHora() {
        when(bloqueRepo.findByUsuarioId(3L)).thenReturn(List.of(
                Datos.bloque(usuario, DayOfWeek.TUESDAY, 8, 9),
                Datos.bloque(usuario, DayOfWeek.MONDAY, 10, 12),
                Datos.bloque(usuario, DayOfWeek.MONDAY, 8, 10)));

        List<BloqueResponse> res = service.listar(CORREO);

        assertEquals(3, res.size());
        assertEquals(DayOfWeek.MONDAY, res.get(0).diaSemana());
        assertEquals(LocalTime.of(8, 0), res.get(0).horaInicio());
        assertEquals(LocalTime.of(10, 0), res.get(1).horaInicio());
        assertEquals(DayOfWeek.TUESDAY, res.get(2).diaSemana());
    }

    @Test
    void listar_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepo.findByCorreo("nadie@utp.edu.pe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.listar("nadie@utp.edu.pe"));
    }

    // ---------- agregar ----------

    @Test
    void agregar_ok_guardaElBloque() {
        when(bloqueRepo.existsByUsuarioIdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
                3L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(8, 0))).thenReturn(false);
        when(bloqueRepo.save(any(BloqueNoDisponible.class))).thenAnswer(inv -> inv.getArgument(0));

        BloqueResponse res = service.agregar(CORREO, bloque(DayOfWeek.MONDAY, 8, 10));

        assertEquals(DayOfWeek.MONDAY, res.diaSemana());
        assertEquals("Motivo", res.motivo());
        verify(bloqueRepo).save(any(BloqueNoDisponible.class));
    }

    @Test
    void agregar_horaInicioPosteriorAFin_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> service.agregar(CORREO, bloque(DayOfWeek.MONDAY, 10, 8)));
        verify(bloqueRepo, never()).save(any());
    }

    @Test
    void agregar_horaInicioIgualAFin_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> service.agregar(CORREO, bloque(DayOfWeek.MONDAY, 8, 8)));
    }

    @Test
    void agregar_conCruce_lanzaReglaNegocio() {
        when(bloqueRepo.existsByUsuarioIdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
                3L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(8, 0))).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> service.agregar(CORREO, bloque(DayOfWeek.MONDAY, 8, 10)));
        verify(bloqueRepo, never()).save(any());
    }

    // ---------- reemplazar ----------

    @Test
    void reemplazar_ok_borraGuardaYDevuelveOrdenado() {
        when(bloqueRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<BloqueResponse> res = service.reemplazar(CORREO, List.of(
                bloque(DayOfWeek.TUESDAY, 8, 10),
                bloque(DayOfWeek.MONDAY, 8, 10)));

        verify(bloqueRepo).deleteByUsuarioId(3L);
        verify(bloqueRepo).flush();
        assertEquals(2, res.size());
        assertEquals(DayOfWeek.MONDAY, res.get(0).diaSemana());
        assertEquals(DayOfWeek.TUESDAY, res.get(1).diaSemana());
    }

    @Test
    void reemplazar_listaVacia_limpiaElHorario() {
        when(bloqueRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<BloqueResponse> res = service.reemplazar(CORREO, List.of());

        verify(bloqueRepo).deleteByUsuarioId(3L);
        assertTrue(res.isEmpty());
    }

    @Test
    void reemplazar_bloquesQueSoloSeTocan_noSeConsideranCruce() {
        when(bloqueRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<BloqueResponse> res = service.reemplazar(CORREO, List.of(
                bloque(DayOfWeek.MONDAY, 8, 10),
                bloque(DayOfWeek.MONDAY, 10, 12)));

        assertEquals(2, res.size());
    }

    @Test
    void reemplazar_mismaHoraEnDiasDistintos_esValido() {
        when(bloqueRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<BloqueResponse> res = service.reemplazar(CORREO, List.of(
                bloque(DayOfWeek.MONDAY, 8, 10),
                bloque(DayOfWeek.TUESDAY, 8, 10)));

        assertEquals(2, res.size());
    }

    @Test
    void reemplazar_conCrucesInternos_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> service.reemplazar(CORREO, List.of(
                bloque(DayOfWeek.MONDAY, 8, 11),
                bloque(DayOfWeek.MONDAY, 10, 12))));
        verify(bloqueRepo, never()).deleteByUsuarioId(any());
    }

    @Test
    void reemplazar_conRangoInvalido_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> service.reemplazar(CORREO, List.of(
                bloque(DayOfWeek.MONDAY, 12, 8))));
    }

    // ---------- límite de horas ocupadas (solo alumnos) ----------

    @Test
    void agregar_alumnoQueSuperaElLimite_lanzaReglaNegocio() {
        // ya tiene 18 h guardadas; un bloque de 3 h pasaría las 20 h
        when(bloqueRepo.findByUsuarioId(3L)).thenReturn(List.of(
                Datos.bloque(usuario, DayOfWeek.MONDAY, 6, 15),
                Datos.bloque(usuario, DayOfWeek.TUESDAY, 6, 15)));

        assertThrows(ReglaNegocioException.class,
                () -> service.agregar(CORREO, bloque(DayOfWeek.WEDNESDAY, 8, 11)));
        verify(bloqueRepo, never()).save(any());
    }

    @Test
    void agregar_alumnoJustoEnElLimite_esValido() {
        when(bloqueRepo.findByUsuarioId(3L)).thenReturn(List.of(
                Datos.bloque(usuario, DayOfWeek.MONDAY, 6, 15),
                Datos.bloque(usuario, DayOfWeek.TUESDAY, 6, 15)));
        when(bloqueRepo.save(any(BloqueNoDisponible.class))).thenAnswer(inv -> inv.getArgument(0));

        BloqueResponse res = service.agregar(CORREO, bloque(DayOfWeek.WEDNESDAY, 8, 10)); // 18 + 2 = 20

        assertEquals(DayOfWeek.WEDNESDAY, res.diaSemana());
    }

    @Test
    void agregar_docenteNoTieneLimite() {
        Usuario docente = Datos.usuario(4L, Rol.DOCENTE, "docente@utp.edu.pe");
        when(usuarioRepo.findByCorreo("docente@utp.edu.pe")).thenReturn(Optional.of(docente));
        when(bloqueRepo.findByUsuarioId(4L)).thenReturn(List.of(
                Datos.bloque(docente, DayOfWeek.MONDAY, 6, 23),
                Datos.bloque(docente, DayOfWeek.TUESDAY, 6, 23)));
        when(bloqueRepo.save(any(BloqueNoDisponible.class))).thenAnswer(inv -> inv.getArgument(0));

        BloqueResponse res = service.agregar("docente@utp.edu.pe", bloque(DayOfWeek.WEDNESDAY, 6, 23));

        assertEquals(DayOfWeek.WEDNESDAY, res.diaSemana());
    }

    @Test
    void reemplazar_alumnoQueSuperaElLimite_lanzaReglaNegocioYNoBorra() {
        assertThrows(ReglaNegocioException.class, () -> service.reemplazar(CORREO, List.of(
                bloque(DayOfWeek.MONDAY, 6, 16),
                bloque(DayOfWeek.TUESDAY, 6, 16),
                bloque(DayOfWeek.WEDNESDAY, 6, 9))));   // 10 + 10 + 3 = 23 h > 20 h
        verify(bloqueRepo, never()).deleteByUsuarioId(any());
    }

    @Test
    void resumen_alumno_devuelveHorasOcupadasYRestantes() {
        when(bloqueRepo.findByUsuarioId(3L)).thenReturn(List.of(
                Datos.bloque(usuario, DayOfWeek.MONDAY, 8, 12),
                Datos.bloque(usuario, DayOfWeek.TUESDAY, 8, 10)));

        DisponibilidadResumenResponse res = service.resumen(CORREO);

        assertTrue(res.aplicaLimite());
        assertEquals(20, res.limiteHoras());
        assertEquals(6.0, res.horasOcupadas());
        assertEquals(14.0, res.horasRestantes());
    }

    @Test
    void resumen_docente_noAplicaLimite() {
        Usuario docente = Datos.usuario(4L, Rol.DOCENTE, "docente@utp.edu.pe");
        when(usuarioRepo.findByCorreo("docente@utp.edu.pe")).thenReturn(Optional.of(docente));
        when(bloqueRepo.findByUsuarioId(4L)).thenReturn(List.of());

        DisponibilidadResumenResponse res = service.resumen("docente@utp.edu.pe");

        assertFalse(res.aplicaLimite());
        assertNull(res.limiteHoras());
        assertNull(res.horasRestantes());
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_bloquePropio_loBorra() {
        BloqueNoDisponible b = Datos.bloque(usuario, DayOfWeek.MONDAY, 8, 10);
        when(bloqueRepo.findById(7L)).thenReturn(Optional.of(b));

        service.eliminar(CORREO, 7L);

        verify(bloqueRepo).delete(b);
    }

    @Test
    void eliminar_bloqueInexistente_lanzaNoEncontrado() {
        when(bloqueRepo.findById(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(CORREO, 7L));
    }

    @Test
    void eliminar_bloqueDeOtroUsuario_lanzaNoEncontrado() {
        Usuario otro = Datos.usuario(99L, Rol.ALUMNO, "otro@utp.edu.pe");
        when(bloqueRepo.findById(7L)).thenReturn(Optional.of(Datos.bloque(otro, DayOfWeek.MONDAY, 8, 10)));

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(CORREO, 7L));
        verify(bloqueRepo, never()).delete(any());
    }
}
