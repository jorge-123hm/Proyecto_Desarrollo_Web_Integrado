package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reprogramacion_api.dto.request.UsuarioRequest;
import reprogramacion_api.dto.response.UsuarioResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol rol) {
        List<Usuario> lista = (rol == null) ? repo.findAll() : repo.findByRol(rol);
        return lista.stream().map(UsuarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new ReglaNegocioException("La contraseña es obligatoria");
        }
        if (repo.existsByCorreo(req.correo())) {
            throw new ReglaNegocioException("Ya existe un usuario con el correo " + req.correo());
        }
        if (repo.existsByCui(req.cui())) {
            throw new ReglaNegocioException("Ya existe un usuario con el CUI " + req.cui());
        }

        Usuario u = Usuario.builder()
                .cui(req.cui())
                .nombres(req.nombres())
                .apellidos(req.apellidos())
                .correo(req.correo())
                .password(encoder.encode(req.password()))
                .rol(req.rol())
                .activo(req.activo() == null || req.activo())
                .build();

        return UsuarioResponse.from(repo.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req) {
        Usuario u = buscar(id);

        if (repo.existsByCorreoAndIdNot(req.correo(), id)) {
            throw new ReglaNegocioException("Ya existe otro usuario con el correo " + req.correo());
        }
        if (repo.existsByCuiAndIdNot(req.cui(), id)) {
            throw new ReglaNegocioException("Ya existe otro usuario con el CUI " + req.cui());
        }

        u.setCui(req.cui());
        u.setNombres(req.nombres());
        u.setApellidos(req.apellidos());
        u.setCorreo(req.correo());
        u.setRol(req.rol());
        if (req.activo() != null) {
            u.setActivo(req.activo());
        }
        if (req.password() != null && !req.password().isBlank()) {
            u.setPassword(encoder.encode(req.password()));
        }

        return UsuarioResponse.from(repo.save(u));
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario u = buscar(id);
        repo.delete(u);
        repo.flush(); // fuerza el error de integridad aquí si tiene datos asociados
    }

    private Usuario buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}