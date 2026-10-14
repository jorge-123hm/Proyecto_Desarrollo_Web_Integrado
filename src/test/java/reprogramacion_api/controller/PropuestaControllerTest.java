package reprogramacion_api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reprogramacion_api.dto.request.PropuestaRequest;
import reprogramacion_api.dto.response.PropuestaResponse;
import reprogramacion_api.service.OpcionesHorarioService;
import reprogramacion_api.service.PropuestaService;
import reprogramacion_api.service.algoritmo.OpcionHorario;
import reprogramacion_api.support.Datos;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropuestaControllerTest {

    private static final String DOCENTE = "docente@utp.edu.pe";

    @Mock
    private PropuestaService propuestaService;
    @Mock
    private OpcionesHorarioService opcionesService;
    @InjectMocks
    private PropuestaController controller;

    @Test
    void evaluar_delegaEnElServicioDeOpciones() {
        LocalDate fecha = LocalDate.of(2026, 10, 20);
        OpcionHorario opcion = new OpcionHorario(fecha, DayOfWeek.TUESDAY, LocalTime.of(8, 0),
                LocalTime.of(10, 0), 3, 4, 75.0, List.of(4L), true);
        when(opcionesService.evaluar(DOCENTE, 1L, fecha, LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .thenReturn(opcion);

        OpcionHorario res = controller.evaluar(Datos.auth(DOCENTE, "DOCENTE"), 1L, fecha,
                LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertSame(opcion, res);
    }

    @Test
    void crear_respondeConEstado201() {
        PropuestaRequest req = new PropuestaRequest(1L, LocalDate.of(2026, 10, 20),
                LocalTime.of(8, 0), LocalTime.of(10, 0), false);
        PropuestaResponse resp = new PropuestaResponse(7L, 1L, "Web", "A", LocalDate.of(2026, 10, 15),
                LocalTime.of(8, 0), LocalTime.of(10, 0), LocalDate.of(2026, 10, 20),
                LocalTime.of(8, 0), LocalTime.of(10, 0), 75.0, 1, false);
        when(propuestaService.crear(DOCENTE, req)).thenReturn(resp);

        ResponseEntity<PropuestaResponse> res = controller.crear(Datos.auth(DOCENTE, "DOCENTE"), req);

        assertEquals(201, res.getStatusCode().value());
        assertSame(resp, res.getBody());
    }
}
