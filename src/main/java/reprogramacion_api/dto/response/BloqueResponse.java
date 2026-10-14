package reprogramacion_api.dto.response;

import reprogramacion_api.model.BloqueNoDisponible;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record BloqueResponse(
        Long id,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo) {

    public static BloqueResponse from(BloqueNoDisponible b) {
        return new BloqueResponse(b.getId(), b.getDiaSemana(),
                b.getHoraInicio(), b.getHoraFin(), b.getMotivo());
    }
}