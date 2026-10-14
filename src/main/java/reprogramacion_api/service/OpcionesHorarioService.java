package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.repository.BloqueNoDisponibleRepository;
import reprogramacion_api.repository.ClaseRepository;
import reprogramacion_api.repository.MatriculaRepository;
import reprogramacion_api.service.algoritmo.CalculadorDisponibilidad;
import reprogramacion_api.service.algoritmo.Intervalo;
import reprogramacion_api.service.algoritmo.OpcionHorario;
import reprogramacion_api.service.algoritmo.ParametrosBusqueda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpcionesHorarioService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final int MAX_DIAS_RANGO = 60;
    private static final int LIMITE_DEFECTO = 10;
    private static final int LIMITE_MAXIMO = 50;

    private final ClaseRepository claseRepo;
    private final MatriculaRepository matriculaRepo;
    private final BloqueNoDisponibleRepository bloqueRepo;
    private final CalculadorDisponibilidad calculador;

    /** Datos que necesita el algoritmo para una clase concreta */
    private record Contexto(Clase clase, List<Intervalo> bloquesDocente, Map<Long, List<Intervalo>> alumnos) {}

    /** RF004: mejores franjas sugeridas */
    @Transactional(readOnly = true)
    public List<OpcionHorario> calcular(String correoDocente, Long claseId,
                                        LocalDate desde, LocalDate hasta, Integer limite) {
        Contexto ctx = cargarContexto(correoDocente, claseId);

        LocalDate inicio = (desde != null) ? desde : LocalDate.now(ZONA).plusDays(1);
        LocalDate fin = (hasta != null) ? hasta : inicio.plusDays(13);
        if (ChronoUnit.DAYS.between(inicio, fin) > MAX_DIAS_RANGO) {
            throw new ReglaNegocioException("El rango de búsqueda no puede superar " + MAX_DIAS_RANGO + " días");
        }
        int tope = (limite == null) ? LIMITE_DEFECTO : Math.min(Math.max(limite, 1), LIMITE_MAXIMO);

        Duration duracion = Duration.between(ctx.clase().getHoraInicio(), ctx.clase().getHoraFin());
        ParametrosBusqueda params = ParametrosBusqueda.porDefecto(inicio, fin, duracion, tope);

        return calculador.calcular(params, ctx.bloquesDocente(), ctx.alumnos());
    }

    /** RF005: evalúa una franja concreta (fecha manual o confirmación de una sugerida) */
    @Transactional(readOnly = true)
    public OpcionHorario evaluar(String correoDocente, Long claseId,
                                 LocalDate fecha, LocalTime inicio, LocalTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new ReglaNegocioException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (!fecha.isAfter(LocalDate.now(ZONA))) {
            throw new ReglaNegocioException("La fecha propuesta debe ser posterior a hoy");
        }

        Contexto ctx = cargarContexto(correoDocente, claseId);
        if (ctx.alumnos().isEmpty()) {
            throw new ReglaNegocioException("El curso no tiene alumnos matriculados");
        }
        return calculador.evaluar(fecha, inicio, fin, ctx.bloquesDocente(), ctx.alumnos());
    }

    private Contexto cargarContexto(String correoDocente, Long claseId) {
        Clase clase = claseRepo.findById(claseId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Clase no encontrada: " + claseId));

        Usuario docente = clase.getCurso().getDocente();
        if (!docente.getCorreo().equals(correoDocente)) {
            throw new AccessDeniedException("La clase no pertenece a este docente");
        }

        List<Long> alumnoIds = matriculaRepo.findByCursoId(clase.getCurso().getId()).stream()
                .map(m -> m.getAlumno().getId())
                .toList();

        List<Long> todos = new ArrayList<>(alumnoIds);
        todos.add(docente.getId());

        Map<Long, List<Intervalo>> bloquesPorUsuario = bloqueRepo.findByUsuarioIdIn(todos).stream()
                .collect(Collectors.groupingBy(
                        b -> b.getUsuario().getId(),
                        Collectors.mapping(
                                b -> new Intervalo(b.getDiaSemana(), b.getHoraInicio(), b.getHoraFin()),
                                Collectors.toList())));

        // Los alumnos sin bloques también entran al mapa (están libres siempre)
        Map<Long, List<Intervalo>> alumnos = new LinkedHashMap<>();
        for (Long id : alumnoIds) {
            alumnos.put(id, bloquesPorUsuario.getOrDefault(id, List.of()));
        }

        return new Contexto(clase, bloquesPorUsuario.getOrDefault(docente.getId(), List.of()), alumnos);
    }
}