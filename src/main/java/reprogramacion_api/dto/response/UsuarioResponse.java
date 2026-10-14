package reprogramacion_api.dto.response;

import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;

public record UsuarioResponse(
        Long id,
        String cui,
        String nombres,
        String apellidos,
        String correo,
        Rol rol,
        boolean activo) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getCui(), u.getNombres(),
                u.getApellidos(), u.getCorreo(), u.getRol(), u.isActivo());
    }
}