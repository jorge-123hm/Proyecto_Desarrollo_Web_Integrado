package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reprogramacion_api.model.Matricula;

import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByCursoId(Long cursoId);
    List<Matricula> findByAlumnoId(Long alumnoId);
    boolean existsByAlumnoIdAndCursoId(Long alumnoId, Long cursoId);
    void deleteByAlumnoIdAndCursoId(Long alumnoId, Long cursoId);
    void deleteByCursoId(Long cursoId);
}
