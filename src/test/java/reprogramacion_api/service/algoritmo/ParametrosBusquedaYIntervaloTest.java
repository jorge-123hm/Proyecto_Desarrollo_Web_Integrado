package reprogramacion_api.service.algoritmo;

import org.junit.jupiter.api.Test;
import reprogramacion_api.exception.ReglaNegocioException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ParametrosBusquedaYIntervaloTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final LocalTime FIN = LocalTime.of(12, 0);
    private static final Set<DayOfWeek> DIAS = EnumSet.of(DayOfWeek.MONDAY);

    private static ParametrosBusqueda crear(LocalDate desde, LocalDate hasta, LocalTime ini, LocalTime fin,
                                            Duration duracion, Duration paso, Set<DayOfWeek> dias, int limite) {
        return new ParametrosBusqueda(desde, hasta, ini, fin, duracion, paso, dias, limite);
    }

    @Test
    void parametrosValidos_seCreanSinError() {
        ParametrosBusqueda p = crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(2), Duration.ofHours(1), DIAS, 5);

        assertEquals(5, p.limite());
        assertEquals(Duration.ofHours(2), p.duracion());
    }

    @Test
    void desdeNulo_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(null, LUNES, INICIO, FIN,
                Duration.ofHours(2), Duration.ofHours(1), DIAS, 5));
    }

    @Test
    void hastaNulo_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, null, INICIO, FIN,
                Duration.ofHours(2), Duration.ofHours(1), DIAS, 5));
    }

    @Test
    void hastaAnteriorADesde_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES.minusDays(1), INICIO, FIN,
                Duration.ofHours(2), Duration.ofHours(1), DIAS, 5));
    }

    @Test
    void jornadaInvertida_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, FIN, INICIO,
                Duration.ofHours(1), Duration.ofHours(1), DIAS, 5));
    }

    @Test
    void duracionCero_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ZERO, Duration.ofHours(1), DIAS, 5));
    }

    @Test
    void duracionNegativa_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(-1), Duration.ofHours(1), DIAS, 5));
    }

    @Test
    void pasoCero_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(1), Duration.ZERO, DIAS, 5));
    }

    @Test
    void pasoNegativo_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(1), Duration.ofHours(-1), DIAS, 5));
    }

    @Test
    void duracionMayorQueLaJornada_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(5), Duration.ofHours(1), DIAS, 5));
    }

    @Test
    void diasNulos_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(2), Duration.ofHours(1), null, 5));
    }

    @Test
    void diasVacios_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(2), Duration.ofHours(1), EnumSet.noneOf(DayOfWeek.class), 5));
    }

    @Test
    void limiteMenorAUno_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> crear(LUNES, LUNES, INICIO, FIN,
                Duration.ofHours(2), Duration.ofHours(1), DIAS, 0));
    }

    @Test
    void porDefecto_usaLunesASabadoDe7A22ConPasoDeUnaHora() {
        ParametrosBusqueda p = ParametrosBusqueda.porDefecto(LUNES, LUNES.plusDays(13), Duration.ofHours(2), 10);

        assertEquals(LocalTime.of(7, 0), p.inicioJornada());
        assertEquals(LocalTime.of(22, 0), p.finJornada());
        assertEquals(Duration.ofHours(1), p.paso());
        assertEquals(6, p.dias().size());
        assertTrue(p.dias().contains(DayOfWeek.SATURDAY));
        assertFalse(p.dias().contains(DayOfWeek.SUNDAY));
    }

    // ---------- Intervalo ----------

    @Test
    void intervalo_solapeReal_seCruza() {
        Intervalo b = new Intervalo(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertTrue(b.seCruzaCon(LocalTime.of(9, 0), LocalTime.of(11, 0)));
        assertTrue(b.seCruzaCon(LocalTime.of(7, 0), LocalTime.of(9, 0)));
        assertTrue(b.seCruzaCon(LocalTime.of(8, 30), LocalTime.of(9, 30)));
    }

    @Test
    void intervalo_bordesQueSoloSeTocan_noSeCruzan() {
        Intervalo b = new Intervalo(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertFalse(b.seCruzaCon(LocalTime.of(10, 0), LocalTime.of(12, 0)));
        assertFalse(b.seCruzaCon(LocalTime.of(6, 0), LocalTime.of(8, 0)));
    }

    @Test
    void intervalo_horariosDisjuntos_noSeCruzan() {
        Intervalo b = new Intervalo(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertFalse(b.seCruzaCon(LocalTime.of(14, 0), LocalTime.of(16, 0)));
    }
}
