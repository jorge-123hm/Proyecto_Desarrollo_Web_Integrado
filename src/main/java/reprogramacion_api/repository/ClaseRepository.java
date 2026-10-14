package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reprogramacion_api.model.Clase;

import java.util.List;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    List<Clase> findByCursoId(Long cursoId);
    List<Clase> findByCursoDocenteCorreoOrderByFechaOriginalAsc(String correo);
}
