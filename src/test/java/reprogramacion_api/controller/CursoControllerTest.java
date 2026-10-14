package reprogramacion_api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reprogramacion_api.dto.request.CursoRequest;
import reprogramacion_api.dto.response.CursoResponse;
import reprogramacion_api.dto.response.UsuarioResponse;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.service.CursoService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoControllerTest {

    @Mock
    private CursoService service;
    @InjectMocks
    private CursoController controller;

    private final CursoResponse curso = new CursoResponse(10L, "Web", "A", "2026-II", 2L, "Pedro H.");
    private final CursoRequest req = new CursoRequest("Web", "A", "2026-II", 2L);

    @Test
    void listar_sinFiltro_delegaEnElServicio() {
        when(service.listar(null)).thenReturn(List.of(curso));

        assertEquals(1, controller.listar(null).size());
    }

    @Test
    void listar_conDocente_delegaConElId() {
        when(service.listar(2L)).thenReturn(List.of(curso));

        assertSame(curso, controller.listar(2L).get(0));
    }

    @Test
    void obtener_delegaEnElServicio() {
        when(service.obtener(10L)).thenReturn(curso);

        assertSame(curso, controller.obtener(10L));
    }

    @Test
    void crear_respondeConEstado201() {
        when(service.crear(req)).thenReturn(curso);

        ResponseEntity<CursoResponse> res = controller.crear(req);

        assertEquals(201, res.getStatusCode().value());
        assertSame(curso, res.getBody());
    }

    @Test
    void actualizar_delegaEnElServicio() {
        when(service.actualizar(10L, req)).thenReturn(curso);

        assertSame(curso, controller.actualizar(10L, req));
    }

    @Test
    void eliminar_respondeConEstado204() {
        ResponseEntity<Void> res = controller.eliminar(10L);

        assertEquals(204, res.getStatusCode().value());
        verify(service).eliminar(10L);
    }

    @Test
    void listarAlumnos_delegaEnElServicio() {
        UsuarioResponse alumno = new UsuarioResponse(3L, "U3", "Luciana", "Canales", "a@utp.edu.pe", Rol.ALUMNO, true);
        when(service.listarAlumnos(10L)).thenReturn(List.of(alumno));

        assertEquals(1, controller.listarAlumnos(10L).size());
    }

    @Test
    void matricular_respondeConEstado201() {
        ResponseEntity<Void> res = controller.matricular(10L, 3L);

        assertEquals(201, res.getStatusCode().value());
        assertNull(res.getBody());
        verify(service).matricular(10L, 3L);
    }

    @Test
    void desmatricular_respondeConEstado204() {
        ResponseEntity<Void> res = controller.desmatricular(10L, 3L);

        assertEquals(204, res.getStatusCode().value());
        verify(service).desmatricular(10L, 3L);
    }
}
