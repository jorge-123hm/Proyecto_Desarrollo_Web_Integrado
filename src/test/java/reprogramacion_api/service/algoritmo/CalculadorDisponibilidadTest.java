package reprogramacion_api.service.algoritmo;

import org.junit.jupiter.api.Test;
import reprogramacion_api.exception.ReglaNegocioException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CalculadorDisponibilidadTest {

    /** 12-oct-2026 es lunes */
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    private final CalculadorDisponibilidad calculador = new CalculadorDisponibilidad();

    /** Solo el lunes, jornada 08-12, clase de 2 h → franjas candidatas: 08-10, 09-11 y 10-12 */
    private static ParametrosBusqueda parametrosLunes(int limite) {
        return new ParametrosBusqueda(LUNES, LUNES,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                Duration.ofHours(2), Duration.ofHours(1),
                EnumSet.of(DayOfWeek.MONDAY), limite);
    }

    private static Intervalo bloqueLunes(int desde, int hasta) {
        return new Intervalo(DayOfWeek.MONDAY, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    @Test
    void todosLibres_dan100PorCiento() {
        var alumnos = Map.of(1L, List.<Intervalo>of(), 2L, List.<Intervalo>of());

        var res = calculador.calcular(parametrosLunes(10), List.of(), alumnos);

        assertEquals(3, res.size());
        assertTrue(res.stream().allMatch(o -> o.porcentaje() == 100.0));
    }

    @Test
    void alumnoOcupado_bajaElPorcentajeYSeOrdenaDescendente() {
        var alumnos = Map.of(
                1L, List.<Intervalo>of(),
                2L, List.<Intervalo>of(),
                3L, List.of(bloqueLunes(8, 10)));

        var res = calculador.calcular(parametrosLunes(10), List.of(), alumnos);

        // 10-12 es la única franja donde los 3 están libres (el bloque termina a las 10)
        assertEquals(LocalTime.of(10, 0), res.get(0).horaInicio());
        assertEquals(100.0, res.get(0).porcentaje());

        // 08-10 y 09-11 dejan a 2 de 3 libres; a igual porcentaje gana la hora más temprana
        assertEquals(LocalTime.of(8, 0), res.get(1).horaInicio());
        assertEquals(66.67, res.get(1).porcentaje());
        assertEquals(List.of(3L), res.get(1).alumnosConConflicto());
        assertEquals(LocalTime.of(9, 0), res.get(2).horaInicio());
    }

    @Test
    void bloqueQueSoloTocaElBorde_noGeneraConflicto() {
        var alumnos = Map.of(1L, List.of(bloqueLunes(10, 12)));

        var res = calculador.calcular(parametrosLunes(10), List.of(), alumnos);

        // La franja 08-10 termina justo cuando empieza el bloque: está libre
        assertEquals(LocalTime.of(8, 0), res.get(0).horaInicio());
        assertEquals(100.0, res.get(0).porcentaje());
    }

    @Test
    void docenteOcupado_descartaLaFranja() {
        var alumnos = Map.of(1L, List.<Intervalo>of(), 2L, List.<Intervalo>of());

        var res = calculador.calcular(parametrosLunes(10), List.of(bloqueLunes(8, 10)), alumnos);

        // Solo sobrevive 10-12
        assertEquals(1, res.size());
        assertEquals(LocalTime.of(10, 0), res.get(0).horaInicio());
        assertTrue(res.get(0).docenteDisponible());
    }

    @Test
    void respetaElLimiteDeResultados() {
        var alumnos = Map.of(1L, List.<Intervalo>of());

        var res = calculador.calcular(parametrosLunes(2), List.of(), alumnos);

        assertEquals(2, res.size());
    }

    @Test
    void soloGeneraFranjasEnLosDiasPermitidos() {
        var params = new ParametrosBusqueda(LUNES, LUNES.plusDays(6),   // lunes a domingo
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                Duration.ofHours(2), Duration.ofHours(1),
                EnumSet.of(DayOfWeek.MONDAY), 50);
        var alumnos = Map.of(1L, List.<Intervalo>of());

        var res = calculador.calcular(params, List.of(), alumnos);

        assertTrue(res.stream().allMatch(o -> o.fecha().equals(LUNES)));
    }

    @Test
    void duracionMayorQueLaJornada_lanzaExcepcion() {
        assertThrows(ReglaNegocioException.class, () -> new ParametrosBusqueda(LUNES, LUNES,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                Duration.ofHours(5), Duration.ofHours(1),
                EnumSet.of(DayOfWeek.MONDAY), 10));
    }

    @Test
    void cursoSinAlumnos_lanzaExcepcion() {
        assertThrows(ReglaNegocioException.class,
                () -> calculador.calcular(parametrosLunes(10), List.of(), Map.of()));
    }

    @Test
    void evaluarFranjaManual_cuentaLosAlumnosConCruce() {
        var alumnos = Map.of(
                1L, List.<Intervalo>of(),
                2L, List.<Intervalo>of(),
                3L, List.of(bloqueLunes(9, 11)));

        var libre = calculador.evaluar(LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), List.of(), alumnos);
        assertEquals(2, libre.alumnosDisponibles());
        assertEquals(3, libre.totalAlumnos());
        assertEquals(List.of(3L), libre.alumnosConConflicto());
        assertTrue(libre.docenteDisponible());

        var docenteOcupado = calculador.evaluar(LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0),
                List.of(bloqueLunes(8, 9)), alumnos);
        assertFalse(docenteOcupado.docenteDisponible());
    }

    /** RNF004: máximo 3 segundos para un curso de hasta 40 alumnos */
    @Test
    void rendimiento_40Alumnos_menosDe3Segundos() {
        Map<Long, List<Intervalo>> alumnos = new HashMap<>();
        for (long id = 1; id <= 40; id++) {
            List<Intervalo> bloques = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                int hora = 7 + (i % 14);
                bloques.add(new Intervalo(DayOfWeek.of(i % 6 + 1),
                        LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0)));
            }
            alumnos.put(id, bloques);
        }
        var params = ParametrosBusqueda.porDefecto(LUNES, LUNES.plusDays(13), Duration.ofHours(2), 10);

        long inicio = System.nanoTime();
        var res = calculador.calcular(params, List.of(), alumnos);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertFalse(res.isEmpty());
        assertTrue(ms < 3000, "El cálculo tardó " + ms + " ms");
    }
}