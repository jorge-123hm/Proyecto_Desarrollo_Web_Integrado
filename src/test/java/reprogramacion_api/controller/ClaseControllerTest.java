package reprogramacion_api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reprogramacion_api.dto.response.ClaseResponse;
import reprogramacion_api.service.ClaseService;
import reprogramacion_api.service.OpcionesHorarioService;
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
class ClaseControllerTest {

    @Mock
    private OpcionesHorarioService opcionesService;
    @Mock
    private ClaseService claseService;
    @InjectMocks
    private ClaseController controller;

    @Test
    void mias_delegaEnElServicioConElCorreoAutenticado() {
        ClaseResponse clase = new ClaseResponse(1L, 10L, "Web", "A", LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(10, 0), "Tema");
        when(claseService.listarDelDocente("docente@utp.edu.pe")).thenReturn(List.of(clase));

        List<ClaseResponse> res = controller.mias(Datos.auth("docente@utp.edu.pe", "DOCENTE"));

        assertEquals(1, res.size());
        assertSame(clase, res.get(0));
    }

    @Test
    void opciones_delegaEnElServicioConLosFiltros() {
        LocalDate desde = LocalDate.of(2026, 10, 14);
        LocalDate hasta = LocalDate.of(2026, 10, 28);
        OpcionHorario opcion = new OpcionHorario(desde, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0),
                LocalTime.of(10, 0), 4, 4, 100.0, List.of(), true);
        when(opcionesService.calcular("docente@utp.edu.pe", 1L, desde, hasta, 5)).thenReturn(List.of(opcion));

        List<OpcionHorario> res = controller.opciones(Datos.auth("docente@utp.edu.pe", "DOCENTE"),
                1L, desde, hasta, 5);

        assertEquals(1, res.size());
        assertEquals(100.0, res.get(0).porcentaje());
    }
}
