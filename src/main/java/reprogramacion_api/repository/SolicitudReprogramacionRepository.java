package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.enums.EstadoSolicitud;

import java.util.List;

public interface SolicitudReprogramacionRepository extends JpaRepository<SolicitudReprogramacion, Long> {

    boolean existsByPropuestaId(Long propuestaId);

    boolean existsByPropuestaClaseIdAndEstado(Long claseId, EstadoSolicitud estado);

    /** Historial con filtros opcionales: cualquier parámetro en null no filtra */
    @Query("""
            select s from SolicitudReprogramacion s
            where (:docenteId is null or s.docente.id = :docenteId)
            and (:cursoId is null or s.propuesta.clase.curso.id = :cursoId)
            and (:estado is null or s.estado = :estado)
            order by s.fechaSolicitud desc
            """)
    List<SolicitudReprogramacion> buscar(@Param("docenteId") Long docenteId,
            @Param("cursoId") Long cursoId,
            @Param("estado") EstadoSolicitud estado);
}