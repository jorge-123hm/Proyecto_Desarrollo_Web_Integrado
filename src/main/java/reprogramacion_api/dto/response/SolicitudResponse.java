package reprogramacion_api.dto.response;

import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.enums.EstadoSolicitud;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SolicitudResponse(
        Long id,
        Long propuestaId,
        Long claseId,
        String curso,
        String seccion,
        String docente,
        LocalDate fechaOriginal,
        LocalTime horaOriginalInicio,
        LocalTime horaOriginalFin,
        LocalDate fechaPropuesta,
        LocalTime horaInicio,
        LocalTime horaFin,
        double porcentajeCoincidencia,
        int alumnosConConflicto,
        EstadoSolicitud estado,
        String observacion,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaResolucion,
        String resueltoPor) {

    public static SolicitudResponse from(SolicitudReprogramacion s) {
        var p = s.getPropuesta();
        var clase = p.getClase();
        var d = s.getDocente();
        var r = s.getResueltoPor();
        return new SolicitudResponse(
                s.getId(), p.getId(), clase.getId(),
                clase.getCurso().getNombre(), clase.getCurso().getSeccion(),
                d.getNombres() + " " + d.getApellidos(),
                clase.getFechaOriginal(), clase.getHoraInicio(), clase.getHoraFin(),
                p.getFechaPropuesta(), p.getHoraInicio(), p.getHoraFin(),
                p.getPorcentajeCoincidencia(), p.getAlumnosConConflicto(),
                s.getEstado(), s.getObservacion(),
                s.getFechaSolicitud(), s.getFechaResolucion(),
                r == null ? null : r.getNombres() + " " + r.getApellidos());
    }
}