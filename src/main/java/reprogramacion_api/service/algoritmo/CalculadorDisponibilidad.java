package reprogramacion_api.service.algoritmo;

import org.springframework.stereotype.Component;
import reprogramacion_api.exception.ReglaNegocioException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class CalculadorDisponibilidad {

    /**
     * Genera las franjas candidatas del rango, descarta las que no sirven al
     * docente
     * y devuelve las mejores ordenadas por cantidad de alumnos libres.
     *
     * @param docente bloques de no disponibilidad del docente
     * @param alumnos alumnoId → sus bloques de no disponibilidad (lista vacía =
     *                siempre libre)
     */
    public List<OpcionHorario> calcular(ParametrosBusqueda p,
            List<Intervalo> docente,
            Map<Long, List<Intervalo>> alumnos) {
        if (alumnos.isEmpty()) {
            throw new ReglaNegocioException("El curso no tiene alumnos matriculados");
        }

        List<OpcionHorario> opciones = new ArrayList<>();

        for (LocalDate fecha = p.desde(); !fecha.isAfter(p.hasta()); fecha = fecha.plusDays(1)) {
            if (!p.dias().contains(fecha.getDayOfWeek()))
                continue;

            LocalTime inicio = p.inicioJornada();
            while (true) {
                LocalTime fin = inicio.plus(p.duracion());
                // fin.isBefore(inicio) detecta que se pasó de medianoche
                if (fin.isBefore(inicio) || fin.isAfter(p.finJornada()))
                    break;

                OpcionHorario o = evaluar(fecha, inicio, fin, docente, alumnos);
                if (o.docenteDisponible()) {
                    opciones.add(o);
                }

                LocalTime siguiente = inicio.plus(p.paso());
                if (!siguiente.isAfter(inicio))
                    break; // evita bucle infinito por desborde
                inicio = siguiente;
            }
        }

        opciones.sort(Comparator.comparingInt(OpcionHorario::alumnosDisponibles).reversed()
                .thenComparing(OpcionHorario::fecha)
                .thenComparing(OpcionHorario::horaInicio));

        return opciones.size() > p.limite()
                ? new ArrayList<>(opciones.subList(0, p.limite()))
                : opciones;
    }

    /**
     * Evalúa UNA franja concreta. Se usa internamente en calcular()
     * para la fecha manual del docente (cuántos alumnos tienen cruce).
     */
    public OpcionHorario evaluar(LocalDate fecha, LocalTime inicio, LocalTime fin,
            List<Intervalo> docente,
            Map<Long, List<Intervalo>> alumnos) {
        DayOfWeek dia = fecha.getDayOfWeek();

        boolean docenteLibre = estaLibre(docente, dia, inicio, fin);

        List<Long> ocupados = alumnos.entrySet().stream()
                .filter(e -> !estaLibre(e.getValue(), dia, inicio, fin))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        int total = alumnos.size();
        int libres = total - ocupados.size();
        double porcentaje = total == 0 ? 0 : Math.round(libres * 10000.0 / total) / 100.0;

        return new OpcionHorario(fecha, dia, inicio, fin, libres, total, porcentaje, ocupados, docenteLibre);
    }

    private boolean estaLibre(List<Intervalo> bloques, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        return bloques.stream().noneMatch(b -> b.dia() == dia && b.seCruzaCon(inicio, fin));
    }
}