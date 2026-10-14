package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    boolean existsByCui(String cui);
    boolean existsByCorreoAndIdNot(String correo, Long id);
boolean existsByCuiAndIdNot(String cui, Long id);
    List<Usuario> findByRol(Rol rol);
}
