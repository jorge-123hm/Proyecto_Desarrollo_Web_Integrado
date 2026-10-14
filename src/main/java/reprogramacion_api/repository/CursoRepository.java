package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reprogramacion_api.model.Curso;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByDocenteId(Long docenteId);
}
