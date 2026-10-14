package reprogramacion_api.service.algoritmo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record OpcionHorario(
        LocalDate fecha,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        int alumnosDisponibles,
        int totalAlumnos,
        double porcentaje,
        List<Long> alumnosConConflicto,   // ids de los alumnos ocupados en esa franja
        boolean docenteDisponible) {
}