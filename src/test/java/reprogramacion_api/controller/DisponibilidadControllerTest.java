package reprogramacion_api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reprogramacion_api.dto.request.BloqueRequest;
import reprogramacion_api.dto.request.DisponibilidadRequest;
import reprogramacion_api.dto.response.BloqueResponse;
import reprogramacion_api.dto.response.DisponibilidadResumenResponse;
import reprogramacion_api.service.DisponibilidadService;
import reprogramacion_api.support.Datos;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadControllerTest {

    private static final String CORREO = "alumno@utp.edu.pe";

    @Mock
    private DisponibilidadService service;
    @InjectMocks
    private DisponibilidadController controller;

    private final BloqueRequest bloqueReq =
            new BloqueRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "Clase");
    private final BloqueResponse bloqueRes =
            new BloqueResponse(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "Clase");

    @Test
    void listar_usaElCorreoDelUsuarioAutenticado() {
        when(service.listar(CORREO)).thenReturn(List.of(bloqueRes));

        assertEquals(1, controller.listar(Datos.auth(CORREO, "ALUMNO")).size());
    }

    @Test
    void agregar_respondeConEstado201() {
        when(service.agregar(CORREO, bloqueReq)).thenReturn(bloqueRes);

        ResponseEntity<BloqueResponse> res = controller.agregar(Datos.auth(CORREO, "ALUMNO"), bloqueReq);

        assertEquals(201, res.getStatusCode().value());
        assertSame(bloqueRes, res.getBody());
    }

    @Test
    void reemplazar_enviaLaListaDeBloquesAlServicio() {
        when(service.reemplazar(CORREO, List.of(bloqueReq))).thenReturn(List.of(bloqueRes));

        List<BloqueResponse> res = controller.reemplazar(Datos.auth(CORREO, "ALUMNO"),
                new DisponibilidadRequest(List.of(bloqueReq)));

        assertEquals(1, res.size());
    }

    @Test
    void eliminar_respondeConEstado204() {
        ResponseEntity<Void> res = controller.eliminar(Datos.auth(CORREO, "ALUMNO"), 1L);

        assertEquals(204, res.getStatusCode().value());
        verify(service).eliminar(CORREO, 1L);
    }

    @Test
    void resumen_usaElCorreoDelUsuarioAutenticado() {
        DisponibilidadResumenResponse resumen = DisponibilidadResumenResponse.de(360, 20);
        when(service.resumen(CORREO)).thenReturn(resumen);

        assertSame(resumen, controller.resumen(Datos.auth(CORREO, "ALUMNO")));
    }
}
