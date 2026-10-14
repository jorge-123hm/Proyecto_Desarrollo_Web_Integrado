package reprogramacion_api.dto.request;

import jakarta.validation.constraints.NotNull;

public record SolicitudRequest(@NotNull Long propuestaId) {
}