package reprogramacion_api.service.algoritmo;

import reprogramacion_api.exception.ReglaNegocioException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

public record ParametrosBusqueda(
        LocalDate desde,
        LocalDate hasta,
        LocalTime inicioJornada,
        LocalTime finJornada,
        Duration duracion,
        Duration paso,
        Set<DayOfWeek> dias,
        int limite) {

    public ParametrosBusqueda {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaNegocioException("El rango de fechas no es válido");
        }
        if (!inicioJornada.isBefore(finJornada)) {
            throw new ReglaNegocioException("La jornada de búsqueda no es válida");
        }
        if (duracion.isZero() || duracion.isNegative() || paso.isZero() || paso.isNegative()) {
            throw new ReglaNegocioException("La duración y el paso deben ser positivos");
        }
        if (duracion.compareTo(Duration.between(inicioJornada, finJornada)) > 0) {
            throw new ReglaNegocioException("La duración de la clase excede la jornada de búsqueda");
        }
        if (dias == null || dias.isEmpty()) {
            throw new ReglaNegocioException("Debe haber al menos un día permitido");
        }
        if (limite < 1) {
            throw new ReglaNegocioException("El límite debe ser al menos 1");
        }
    }

    /** Lunes a sábado, de 07:00 a 22:00, franjas que arrancan cada hora */
    public static ParametrosBusqueda porDefecto(LocalDate desde, LocalDate hasta,
                                                Duration duracion, int limite) {
        return new ParametrosBusqueda(desde, hasta,
                LocalTime.of(7, 0), LocalTime.of(22, 0),
                duracion, Duration.ofHours(1),
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), limite);
    }
}