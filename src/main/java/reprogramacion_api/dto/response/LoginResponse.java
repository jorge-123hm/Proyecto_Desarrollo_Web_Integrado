package reprogramacion_api.dto.response;

import reprogramacion_api.model.enums.Rol;

public record LoginResponse(
        String token,
        String tipo,
        Long usuarioId,
        String nombres,
        String correo,
        Rol rol) {
}
