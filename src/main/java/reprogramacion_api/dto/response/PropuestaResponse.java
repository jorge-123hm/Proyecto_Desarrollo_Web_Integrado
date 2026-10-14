package reprogramacion_api.dto.response;

import reprogramacion_api.model.PropuestaReprogramacion;

import java.time.LocalDate;
import java.time.LocalTime;

public record PropuestaResponse(
        Long id,
        Long claseId,
        String curso,
        String seccion,
        LocalDate fechaOriginal,
        LocalTime horaOriginalInicio,
        LocalTime horaOriginalFin,
        LocalDate fechaPropuesta,
        LocalTime horaInicio,
        LocalTime horaFin,
        double porcentajeCoincidencia,
        int alumnosConConflicto,
        boolean manual) {

    public static PropuestaResponse from(PropuestaReprogramacion p) {
        var clase = p.getClase();
        return new PropuestaResponse(
                p.getId(), clase.getId(),
                clase.getCurso().getNombre(), clase.getCurso().getSeccion(),
                clase.getFechaOriginal(), clase.getHoraInicio(), clase.getHoraFin(),
                p.getFechaPropuesta(), p.getHoraInicio(), p.getHoraFin(),
                p.getPorcentajeCoincidencia(), p.getAlumnosConConflicto(), p.isManual());
    }
}