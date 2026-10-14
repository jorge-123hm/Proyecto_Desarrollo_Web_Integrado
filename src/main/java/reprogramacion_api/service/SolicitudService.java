package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reprogramacion_api.dto.response.SolicitudResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.PropuestaReprogramacion;
import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.EstadoSolicitud;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.MatriculaRepository;
import reprogramacion_api.repository.PropuestaReprogramacionRepository;
import reprogramacion_api.repository.SolicitudReprogramacionRepository;
import reprogramacion_api.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final SolicitudReprogramacionRepository solicitudRepo;
    private final PropuestaReprogramacionRepository propuestaRepo;
    private final UsuarioRepository usuarioRepo;
    private final MatriculaRepository matriculaRepo;
    private final NotificacionService notificacionService;

    // ---------- CU005: enviar ----------

    @Transactional
    public SolicitudResponse enviar(String correoDocente, Long propuestaId) {
        Usuario docente = usuario(correoDocente);

        PropuestaReprogramacion p = propuestaRepo.findById(propuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propuesta no encontrada: " + propuestaId));

        if (!p.getClase().getCurso().getDocente().getId().equals(docente.getId())) {
            throw new AccessDeniedException("La propuesta no pertenece a este docente");
        }
        if (solicitudRepo.existsByPropuestaId(propuestaId)) {
            throw new ReglaNegocioException("Esta propuesta ya fue enviada como solicitud");
        }
        if (solicitudRepo.existsByPropuestaClaseIdAndEstado(p.getClase().getId(), EstadoSolicitud.PENDIENTE)) {
            throw new ReglaNegocioException("Ya existe una solicitud pendiente para esta clase");
        }
        if (!p.getFechaPropuesta().isAfter(LocalDate.now(ZONA))) {
            throw new ReglaNegocioException("La fecha propuesta ya pasó; genere una nueva propuesta");
        }

        SolicitudReprogramacion guardada = solicitudRepo.save(SolicitudReprogramacion.builder()
                .propuesta(p)
                .docente(docente)
                .estado(EstadoSolicitud.PENDIENTE)
                .build());

        // Avisa a coordinación que hay una solicitud nueva
        Clase clase = p.getClase();
        notificacionService.notificarVarios(
                usuarioRepo.findByRol(Rol.COORDINACION), guardada,
                "Nueva solicitud de " + nombre(docente) + " para reprogramar "
                        + clase.getCurso().getNombre() + " (sección " + clase.getCurso().getSeccion() + ")");

        return SolicitudResponse.from(guardada);
    }

    // ---------- CU006: aprobar o rechazar ----------

    @Transactional
    public SolicitudResponse resolver(String correoCoordinacion, Long id,
                                      EstadoSolicitud decision, String observacion) {
        if (decision == EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("La decisión debe ser APROBADA o RECHAZADA");
        }
        String obs = (observacion == null || observacion.isBlank()) ? null : observacion.trim();
        if (decision == EstadoSolicitud.RECHAZADA && obs == null) {
            throw new ReglaNegocioException("Debe indicar el motivo del rechazo");
        }

        Usuario coordinador = usuario(correoCoordinacion);
        SolicitudReprogramacion s = buscar(id);

        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("La solicitud ya fue resuelta (" + s.getEstado() + ")");
        }

        PropuestaReprogramacion p = s.getPropuesta();
        if (decision == EstadoSolicitud.APROBADA && !p.getFechaPropuesta().isAfter(LocalDate.now(ZONA))) {
            throw new ReglaNegocioException("La fecha propuesta ya pasó; rechace la solicitud para que el docente envíe otra");
        }

        s.setEstado(decision);
        s.setObservacion(obs);
        s.setFechaResolucion(LocalDateTime.now(ZONA));
        s.setResueltoPor(coordinador);
        SolicitudReprogramacion guardada = solicitudRepo.save(s);

        notificarResolucion(guardada, obs);
        return SolicitudResponse.from(guardada);
    }

    private void notificarResolucion(SolicitudReprogramacion s, String obs) {
        PropuestaReprogramacion p = s.getPropuesta();
        Clase clase = p.getClase();
        String curso = clase.getCurso().getNombre() + " (sección " + clase.getCurso().getSeccion() + ")";

        if (s.getEstado() == EstadoSolicitud.APROBADA) {
            String nueva = p.getFechaPropuesta().format(FECHA) + " de "
                    + p.getHoraInicio().format(HORA) + " a " + p.getHoraFin().format(HORA);

            // Docente
            notificacionService.notificarVarios(List.of(s.getDocente()), s,
                    "Su solicitud fue APROBADA: la clase de " + curso + " será el " + nueva);

            // Alumnos matriculados: reciben la fecha definitiva
            List<Usuario> alumnos = matriculaRepo.findByCursoId(clase.getCurso().getId()).stream()
                    .map(m -> m.getAlumno())
                    .toList();
            notificacionService.notificarVarios(alumnos, s,
                    "La clase de " + curso + " del " + clase.getFechaOriginal().format(FECHA)
                            + " fue reprogramada para el " + nueva);
        } else {
            notificacionService.notificarVarios(List.of(s.getDocente()), s,
                    "Su solicitud para " + curso + " fue RECHAZADA. Motivo: " + obs);
        }
    }

    // ---------- CU007: historial y detalle ----------

    /** Docente: solo las suyas. Coordinación: todas. Filtros opcionales por estado y curso */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> historial(String correo, boolean esCoordinacion,
                                             EstadoSolicitud estado, Long cursoId) {
        Long docenteId = esCoordinacion ? null : usuario(correo).getId();
        return solicitudRepo.buscar(docenteId, cursoId, estado).stream()
                .map(SolicitudResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SolicitudResponse obtener(String correo, boolean esCoordinacion, Long id) {
        SolicitudReprogramacion s = buscar(id);
        if (!esCoordinacion && !s.getDocente().getCorreo().equals(correo)) {
            throw new AccessDeniedException("La solicitud no pertenece a este docente");
        }
        return SolicitudResponse.from(s);
    }

    // ---------- Auxiliares ----------

    private SolicitudReprogramacion buscar(Long id) {
        return solicitudRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada: " + id));
    }

    private Usuario usuario(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private String nombre(Usuario u) {
        return u.getNombres() + " " + u.getApellidos();
    }
}