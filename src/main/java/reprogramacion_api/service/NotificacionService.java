package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reprogramacion_api.dto.response.NotificacionResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.model.Notificacion;
import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.repository.NotificacionRepository;
import reprogramacion_api.repository.UsuarioRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repo;
    private final UsuarioRepository usuarioRepo;

    // ---------- Generación (la usa SolicitudService) ----------

    @Transactional
    public void notificarVarios(Collection<Usuario> destinatarios,
                                SolicitudReprogramacion solicitud, String mensaje) {
        List<Notificacion> lista = destinatarios.stream()
                .map(u -> Notificacion.builder()
                        .usuario(u).solicitud(solicitud).mensaje(mensaje).build())
                .toList();
        repo.saveAll(lista);
    }

    // ---------- Consulta (la usa el controlador) ----------

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(String correo) {
        return repo.findByUsuarioIdOrderByFechaDesc(usuario(correo).getId()).stream()
                .map(NotificacionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> contarNoLeidas(String correo) {
        return Map.of("cantidad", repo.countByUsuarioIdAndLeidaFalse(usuario(correo).getId()));
    }

    @Transactional
    public NotificacionResponse marcarLeida(String correo, Long id) {
        Notificacion n = repo.findByIdAndUsuarioId(id, usuario(correo).getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada: " + id));
        n.setLeida(true);
        return NotificacionResponse.from(repo.save(n));
    }

    private Usuario usuario(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}