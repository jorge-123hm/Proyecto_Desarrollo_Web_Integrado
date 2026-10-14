package reprogramacion_api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reprogramacion_api.dto.request.UsuarioRequest;
import reprogramacion_api.dto.response.UsuarioResponse;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.service.UsuarioService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService service;
    @InjectMocks
    private UsuarioController controller;

    private final UsuarioResponse usuario =
            new UsuarioResponse(1L, "U1", "Ana", "Ruiz", "ana@utp.edu.pe", Rol.ALUMNO, true);
    private final UsuarioRequest req =
            new UsuarioRequest("U1", "Ana", "Ruiz", "ana@utp.edu.pe", "123456", Rol.ALUMNO, true);

    @Test
    void listar_sinRol_delegaEnElServicio() {
        when(service.listar(null)).thenReturn(List.of(usuario));

        assertEquals(1, controller.listar(null).size());
    }

    @Test
    void listar_conRol_delegaConElRol() {
        when(service.listar(Rol.ALUMNO)).thenReturn(List.of(usuario));

        assertSame(usuario, controller.listar(Rol.ALUMNO).get(0));
    }

    @Test
    void obtener_delegaEnElServicio() {
        when(service.obtener(1L)).thenReturn(usuario);

        assertSame(usuario, controller.obtener(1L));
    }

    @Test
    void crear_respondeConEstado201() {
        when(service.crear(req)).thenReturn(usuario);

        ResponseEntity<UsuarioResponse> res = controller.crear(req);

        assertEquals(201, res.getStatusCode().value());
        assertSame(usuario, res.getBody());
    }

    @Test
    void actualizar_delegaEnElServicio() {
        when(service.actualizar(1L, req)).thenReturn(usuario);

        assertSame(usuario, controller.actualizar(1L, req));
    }

    @Test
    void eliminar_respondeConEstado204() {
        ResponseEntity<Void> res = controller.eliminar(1L);

        assertEquals(204, res.getStatusCode().value());
        verify(service).eliminar(1L);
    }
}
