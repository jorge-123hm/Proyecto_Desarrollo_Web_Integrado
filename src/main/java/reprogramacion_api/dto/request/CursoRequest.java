package reprogramacion_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CursoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 20) String seccion,
        @NotBlank @Size(max = 20) String periodo,
        @NotNull Long docenteId) {
}