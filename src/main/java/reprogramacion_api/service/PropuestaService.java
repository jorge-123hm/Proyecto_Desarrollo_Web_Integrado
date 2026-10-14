package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reprogramacion_api.dto.request.PropuestaRequest;
import reprogramacion_api.dto.response.PropuestaResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.PropuestaReprogramacion;
import reprogramacion_api.repository.ClaseRepository;
import reprogramacion_api.repository.PropuestaReprogramacionRepository;
import reprogramacion_api.service.algoritmo.OpcionHorario;

@Service
@RequiredArgsConstructor
public class PropuestaService {

    private final PropuestaReprogramacionRepository propuestaRepo;
    private final ClaseRepository claseRepo;
    private final OpcionesHorarioService opcionesService;

    @Transactional
    public PropuestaResponse crear(String correoDocente, PropuestaRequest req) {
        // Valida dueño de la clase, fecha futura y rango de horas; calcula el cruce real
        OpcionHorario eval = opcionesService.evaluar(correoDocente, req.claseId(),
                req.fecha(), req.horaInicio(), req.horaFin());

        if (!eval.docenteDisponible()) {
            throw new ReglaNegocioException(
                    "No puede proponer una franja en la que usted tiene un bloque de no disponibilidad");
        }

        Clase clase = claseRepo.findById(req.claseId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Clase no encontrada: " + req.claseId()));

        PropuestaReprogramacion p = PropuestaReprogramacion.builder()
                .clase(clase)
                .fechaPropuesta(eval.fecha())
                .horaInicio(eval.horaInicio())
                .horaFin(eval.horaFin())
                .porcentajeCoincidencia(eval.porcentaje())
                .alumnosConConflicto(eval.alumnosConConflicto().size())
                .manual(Boolean.TRUE.equals(req.manual()))
                .build();

        return PropuestaResponse.from(propuestaRepo.save(p));
    }
}