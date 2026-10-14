package reprogramacion_api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record PropuestaRequest(
        @NotNull Long claseId,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        /** true si el docente escribió la fecha a mano; false si eligió una sugerida */
        Boolean manual) {
}