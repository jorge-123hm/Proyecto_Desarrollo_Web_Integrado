package reprogramacion_api.dto.response;

import reprogramacion_api.model.Clase;

import java.time.LocalDate;
import java.time.LocalTime;

public record ClaseResponse(
        Long id,
        Long cursoId,
        String curso,
        String seccion,
        LocalDate fechaOriginal,
        LocalTime horaInicio,
        LocalTime horaFin,
        String tema) {

    public static ClaseResponse from(Clase c) {
        return new ClaseResponse(c.getId(), c.getCurso().getId(), c.getCurso().getNombre(),
                c.getCurso().getSeccion(), c.getFechaOriginal(), c.getHoraInicio(),
                c.getHoraFin(), c.getTema());
    }
}