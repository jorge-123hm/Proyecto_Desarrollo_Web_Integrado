package reprogramacion_api.dto.response;

import reprogramacion_api.model.Notificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        Long solicitudId,
        String mensaje,
        boolean leida,
        LocalDateTime fecha) {

    public static NotificacionResponse from(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getSolicitud() == null ? null : n.getSolicitud().getId(),
                n.getMensaje(), n.isLeida(), n.getFecha());
    }
}