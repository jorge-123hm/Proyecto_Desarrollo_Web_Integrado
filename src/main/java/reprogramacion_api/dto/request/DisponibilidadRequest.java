package reprogramacion_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DisponibilidadRequest(
        @NotNull List<@Valid BloqueRequest> bloques) {
}