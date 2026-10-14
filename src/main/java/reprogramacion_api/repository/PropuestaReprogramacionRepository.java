package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reprogramacion_api.model.PropuestaReprogramacion;

import java.util.List;

public interface PropuestaReprogramacionRepository extends JpaRepository<PropuestaReprogramacion, Long> {
    List<PropuestaReprogramacion> findByClaseId(Long claseId);
}
