package reprogramacion_api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reprogramacion_api.dto.response.NotificacionResponse;
import reprogramacion_api.service.NotificacionService;
import reprogramacion_api.support.Datos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private static final String CORREO = "alumno@utp.edu.pe";

    @Mock
    private NotificacionService service;
    @InjectMocks
    private NotificacionController controller;

    private final NotificacionResponse notif =
            new NotificacionResponse(1L, 5L, "Mensaje", false, LocalDateTime.now());

    @Test
    void listar_delegaEnElServicio() {
        when(service.listar(CORREO)).thenReturn(List.of(notif));

        assertEquals(1, controller.listar(Datos.auth(CORREO, "ALUMNO")).size());
    }

    @Test
    void cantidadNoLeidas_devuelveElMapaDelServicio() {
        when(service.contarNoLeidas(CORREO)).thenReturn(Map.of("cantidad", 3L));

        Map<String, Long> res = controller.cantidadNoLeidas(Datos.auth(CORREO, "ALUMNO"));

        assertEquals(Long.valueOf(3L), res.get("cantidad"));
    }

    @Test
    void marcarLeida_delegaEnElServicio() {
        when(service.marcarLeida(CORREO, 1L)).thenReturn(notif);

        assertSame(notif, controller.marcarLeida(Datos.auth(CORREO, "ALUMNO"), 1L));
    }
}
