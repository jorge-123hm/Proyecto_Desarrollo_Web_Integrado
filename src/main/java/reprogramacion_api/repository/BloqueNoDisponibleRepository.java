package reprogramacion_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reprogramacion_api.model.BloqueNoDisponible;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface BloqueNoDisponibleRepository extends JpaRepository<BloqueNoDisponible, Long> {

    List<BloqueNoDisponible> findByUsuarioId(Long usuarioId);

    List<BloqueNoDisponible> findByUsuarioIdIn(Collection<Long> usuarioIds);

    void deleteByUsuarioId(Long usuarioId);

    /**
     * Detecta cruces: un bloque existente se cruza con uno nuevo si
     * existente.inicio < nuevo.fin  Y  existente.fin > nuevo.inicio.
     */
    boolean existsByUsuarioIdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long usuarioId, DayOfWeek diaSemana, LocalTime nuevoFin, LocalTime nuevoInicio);
}