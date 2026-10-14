package reprogramacion_api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import reprogramacion_api.model.enums.EstadoSolicitud;

public record ResolverRequest(
        /** APROBADA o RECHAZADA */
        @NotNull EstadoSolicitud decision,
        @Size(max = 500) String observacion) {
}