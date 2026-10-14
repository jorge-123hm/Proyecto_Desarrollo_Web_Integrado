package reprogramacion_api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import reprogramacion_api.dto.request.ResolverRequest;
import reprogramacion_api.dto.request.SolicitudRequest;
import reprogramacion_api.dto.response.SolicitudResponse;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.PropuestaReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.EstadoSolicitud;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.service.SolicitudService;
import reprogramacion_api.support.Datos;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudControllerTest {

    private static final String DOCENTE = "docente@utp.edu.pe";
    private static final String COORD = "coord@utp.edu.pe";

    @Mock
    private SolicitudService service;
    @InjectMocks
    private SolicitudController controller;

    private SolicitudResponse respuesta;

    @BeforeEach
    void setUp() {
        Usuario docente = Datos.usuario(2L, Rol.DOCENTE, DOCENTE);
        Curso curso = Datos.curso(10L, docente);
        Clase clase = Datos.clase(100L, curso);
        PropuestaReprogramacion p = Datos.propuesta(500L, clase, Datos.futuro());
        respuesta = SolicitudResponse.from(Datos.solicitud(900L, p, docente, EstadoSolicitud.PENDIENTE));
    }

    @Test
    void enviar_respondeConEstado201() {
        when(service.enviar(DOCENTE, 500L)).thenReturn(respuesta);

        ResponseEntity<SolicitudResponse> res =
                controller.enviar(Datos.auth(DOCENTE, "DOCENTE"), new SolicitudRequest(500L));

        assertEquals(201, res.getStatusCode().value());
        assertSame(respuesta, res.getBody());
    }

    @Test
    void historial_conRolCoordinacion_marcaEsCoordinacionVerdadero() {
        when(service.historial(COORD, true, EstadoSolicitud.PENDIENTE, 10L)).thenReturn(List.of(respuesta));

        List<SolicitudResponse> res = controller.historial(Datos.auth(COORD, "COORDINACION"),
                EstadoSolicitud.PENDIENTE, 10L);

        assertEquals(1, res.size());
    }

    @Test
    void historial_conRolDocente_marcaEsCoordinacionFalso() {
        when(service.historial(DOCENTE, false, null, null)).thenReturn(List.of(respuesta));

        List<SolicitudResponse> res = controller.historial(Datos.auth(DOCENTE, "DOCENTE"), null, null);

        assertEquals(1, res.size());
    }

    @Test
    void mias_pideSoloLasSolicitudesDelDocente() {
        when(service.historial(DOCENTE, false, null, null)).thenReturn(List.of(respuesta));

        assertEquals(1, controller.mias(Datos.auth(DOCENTE, "DOCENTE")).size());
    }

    @Test
    void detalle_deCoordinacion_usaElIndicadorCorrecto() {
        when(service.obtener(COORD, true, 900L)).thenReturn(respuesta);

        assertSame(respuesta, controller.detalle(Datos.auth(COORD, "COORDINACION"), 900L));
    }

    @Test
    void detalle_deDocente_usaElIndicadorCorrecto() {
        when(service.obtener(DOCENTE, false, 900L)).thenReturn(respuesta);

        assertSame(respuesta, controller.detalle(Datos.auth(DOCENTE, "DOCENTE"), 900L));
    }

    @Test
    void resolver_enviaDecisionYObservacionAlServicio() {
        when(service.resolver(COORD, 900L, EstadoSolicitud.APROBADA, "ok")).thenReturn(respuesta);

        SolicitudResponse res = controller.resolver(Datos.auth(COORD, "COORDINACION"), 900L,
                new ResolverRequest(EstadoSolicitud.APROBADA, "ok"));

        assertSame(respuesta, res);
    }
}
