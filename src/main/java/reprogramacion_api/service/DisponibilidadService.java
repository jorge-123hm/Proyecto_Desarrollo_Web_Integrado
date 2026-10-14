package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reprogramacion_api.dto.request.BloqueRequest;
import reprogramacion_api.dto.response.BloqueResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.BloqueNoDisponible;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.repository.BloqueNoDisponibleRepository;
import reprogramacion_api.repository.UsuarioRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final BloqueNoDisponibleRepository bloqueRepo;
    private final UsuarioRepository usuarioRepo;

    /** Bloques del usuario autenticado, ordenados por día y hora */
    @Transactional(readOnly = true)
    public List<BloqueResponse> listar(String correo) {
        Usuario u = usuario(correo);
        return bloqueRepo.findByUsuarioId(u.getId()).stream()
                .sorted(Comparator.comparing(BloqueNoDisponible::getDiaSemana)
                        .thenComparing(BloqueNoDisponible::getHoraInicio))
                .map(BloqueResponse::from)
                .toList();
    }

    /** Agrega un bloque suelto, rechazando cruces con los ya registrados */
    @Transactional
    public BloqueResponse agregar(String correo, BloqueRequest req) {
        Usuario u = usuario(correo);
        validarRango(req);

        boolean cruza = bloqueRepo
                .existsByUsuarioIdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
                        u.getId(), req.diaSemana(), req.horaFin(), req.horaInicio());
        if (cruza) {
            throw new ReglaNegocioException("El bloque se cruza con otro ya registrado ese día");
        }

        return BloqueResponse.from(bloqueRepo.save(toEntity(u, req)));
    }

    /**
    * Reemplaza el horario completo del usuario por la lista recibida.
     * Es lo más cómodo para la grilla semanal de Angular: el alumno marca
     * y desmarca celdas y al final se guarda todo con un solo botón.
     */
    @Transactional
    public List<BloqueResponse> reemplazar(String correo, List<BloqueRequest> bloques) {
        Usuario u = usuario(correo);

        bloques.forEach(this::validarRango);
        validarSinCrucesInternos(bloques);

        bloqueRepo.deleteByUsuarioId(u.getId());
        bloqueRepo.flush();

        List<BloqueNoDisponible> nuevos = bloques.stream().map(b -> toEntity(u, b)).toList();
        return bloqueRepo.saveAll(nuevos).stream()
                .sorted(Comparator.comparing(BloqueNoDisponible::getDiaSemana)
                        .thenComparing(BloqueNoDisponible::getHoraInicio))
                .map(BloqueResponse::from)
                .toList();
    }

    /** Solo se pueden borrar bloques propios */
    @Transactional
    public void eliminar(String correo, Long bloqueId) {
        Usuario u = usuario(correo);
        BloqueNoDisponible b = bloqueRepo.findById(bloqueId)
                .filter(x -> x.getUsuario().getId().equals(u.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloque no encontrado: " + bloqueId));
        bloqueRepo.delete(b);
    }

    // ---------- Auxiliares ----------

    private Usuario usuario(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private void validarRango(BloqueRequest r) {
        if (!r.horaInicio().isBefore(r.horaFin())) {
            throw new ReglaNegocioException("La hora de inicio debe ser anterior a la hora de fin");
        }
    }

    /** Dentro de la misma lista no puede haber dos bloques que se pisen el mismo día */
    private void validarSinCrucesInternos(List<BloqueRequest> bloques) {
        List<BloqueRequest> ordenados = bloques.stream()
                .sorted(Comparator.comparing(BloqueRequest::diaSemana)
                        .thenComparing(BloqueRequest::horaInicio))
                .toList();

        for (int i = 1; i < ordenados.size(); i++) {
            BloqueRequest anterior = ordenados.get(i - 1);
            BloqueRequest actual = ordenados.get(i);
            if (anterior.diaSemana() == actual.diaSemana()
                    && actual.horaInicio().isBefore(anterior.horaFin())) {
                throw new ReglaNegocioException("Hay bloques que se cruzan el " + actual.diaSemana()
                        + " (" + anterior.horaInicio() + "-" + anterior.horaFin()
                        + " y " + actual.horaInicio() + "-" + actual.horaFin() + ")");
            }
        }
    }

    private BloqueNoDisponible toEntity(Usuario u, BloqueRequest r) {
        return BloqueNoDisponible.builder()
                .usuario(u)
                .diaSemana(r.diaSemana())
                .horaInicio(r.horaInicio())
                .horaFin(r.horaFin())
                .motivo(r.motivo())
                .build();
    }
}