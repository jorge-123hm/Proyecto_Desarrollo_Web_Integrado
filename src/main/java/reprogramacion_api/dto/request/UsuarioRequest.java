package reprogramacion_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import reprogramacion_api.model.enums.Rol;

public record UsuarioRequest(
        @NotBlank @Size(max = 20) String cui,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 120) String correo,
        /** Obligatoria al crear; en la edición, si viene vacía no se cambia */
        String password,
        @NotNull Rol rol,
        Boolean activo) {
}