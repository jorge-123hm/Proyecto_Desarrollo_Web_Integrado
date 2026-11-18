package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.UsuarioRepository;

import java.security.SecureRandom;

/** ALUMNO -> U + 8 dígitos · DOCENTE -> C + 8 dígitos · COORDINACION -> A + 8 dígitos */
@Component
@RequiredArgsConstructor
public class CuiGenerador {

    private final UsuarioRepository repo;
    private final SecureRandom random = new SecureRandom();

    public static char prefijo(Rol rol) {
        return switch (rol) {
            case ALUMNO -> 'U';
            case DOCENTE -> 'C';
            case COORDINACION -> 'A';
        };
    }

    /** true si el código ya tiene el formato (letra + 8 dígitos) que corresponde al rol */
    public boolean coincideConRol(String cui, Rol rol) {
        return cui != null && cui.matches("[A-Z]\\d{8}") && cui.charAt(0) == prefijo(rol);
    }

    public String generar(Rol rol) {
        String cui;
        do {
            cui = prefijo(rol) + String.format("%08d", random.nextInt(100_000_000));
        } while (repo.existsByCui(cui));
        return cui;
    }
}
