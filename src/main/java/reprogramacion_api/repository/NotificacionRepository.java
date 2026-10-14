package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reprogramacion_api.model.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);
}
