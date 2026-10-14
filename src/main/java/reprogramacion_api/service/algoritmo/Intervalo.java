package reprogramacion_api.service.algoritmo;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Bloque semanal de no disponibilidad*/
public record Intervalo(DayOfWeek dia, LocalTime inicio, LocalTime fin) {

    /** Se cruzan solo si hay solape real: los bordes que se tocan no cuentan */
    public boolean seCruzaCon(LocalTime otroInicio, LocalTime otroFin) {
        return inicio.isBefore(otroFin) && fin.isAfter(otroInicio);
    }
}