package reprogramacion_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reprogramacion_api.dto.request.PropuestaRequest;
import reprogramacion_api.dto.response.PropuestaResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.PropuestaReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.ClaseRepository;
import reprogramacion_api.repository.PropuestaReprogramacionRepository;
import reprogramacion_api.service.algoritmo.OpcionHorario;
import reprogramacion_api.support.Datos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropuestaServiceTest {

    private static final String DOCENTE = "docente@utp.edu.pe";

    @Mock
    private PropuestaReprogramacionRepository propuestaRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private OpcionesHorarioService opcionesService;
    @InjectMocks
    private PropuestaService service;

    private LocalDate fecha;
    private Clase clase;

    @BeforeEach
    void setUp() {
        fecha = Datos.futuro();
        Usuario docente = Datos.usuario(2L, Rol.DOCENTE, DOCENTE);
        Curso curso = Datos.curso(10L, docente);
        clase = Datos.clase(100L, curso);
        when(claseRepo.findById(100L)).thenReturn(Optional.of(clase));
        when(propuestaRepo.save(any(PropuestaReprogramacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private OpcionHorario evaluacion(boolean docenteDisponible) {
        return new OpcionHorario(fecha, fecha.getDayOfWeek(), LocalTime.of(14, 0), LocalTime.of(16, 0),
                3, 4, 75.0, List.of(4L), docenteDisponible);
    }

    private PropuestaRequest req(Boolean manual) {
        return new PropuestaRequest(100L, fecha, LocalTime.of(14, 0), LocalTime.of(16, 0), manual);
    }

    @Test
    void crear_ok_copiaLosDatosDeLaEvaluacion() {
        when(opcionesService.evaluar(DOCENTE, 100L, fecha, LocalTime.of(14, 0), LocalTime.of(16, 0)))
                .thenReturn(evaluacion(true));

        PropuestaResponse res = service.crear(DOCENTE, req(true));

        ArgumentCaptor<PropuestaReprogramacion> cap = ArgumentCaptor.forClass(PropuestaReprogramacion.class);
        verify(propuestaRepo).save(cap.capture());
        PropuestaReprogramacion p = cap.getValue();
        assertSame(clase, p.getClase());
        assertEquals(fecha, p.getFechaPropuesta());
        assertEquals(75.0, p.getPorcentajeCoincidencia());
        assertEquals(1, p.getAlumnosConConflicto());
        assertTrue(p.isManual());
        assertEquals(75.0, res.porcentajeCoincidencia());
        assertEquals(100L, res.claseId().longValue());
    }

    @Test
    void crear_manualNulo_seGuardaComoNoManual() {
        when(opcionesService.evaluar(DOCENTE, 100L, fecha, LocalTime.of(14, 0), LocalTime.of(16, 0)))
                .thenReturn(evaluacion(true));

        PropuestaResponse res = service.crear(DOCENTE, req(null));

        assertFalse(res.manual());
    }

    @Test
    void crear_docenteNoDisponible_lanzaReglaNegocio() {
        when(opcionesService.evaluar(DOCENTE, 100L, fecha, LocalTime.of(14, 0), LocalTime.of(16, 0)))
                .thenReturn(evaluacion(false));

        assertThrows(ReglaNegocioException.class, () -> service.crear(DOCENTE, req(false)));
        verify(propuestaRepo, never()).save(any());
    }

    @Test
    void crear_claseInexistente_lanzaNoEncontrado() {
        when(claseRepo.findById(100L)).thenReturn(Optional.empty());
        when(opcionesService.evaluar(DOCENTE, 100L, fecha, LocalTime.of(14, 0), LocalTime.of(16, 0)))
                .thenReturn(evaluacion(true));

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(DOCENTE, req(false)));
    }
}
