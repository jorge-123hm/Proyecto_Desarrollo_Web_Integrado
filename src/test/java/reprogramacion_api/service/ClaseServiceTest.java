package reprogramacion_api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reprogramacion_api.dto.response.ClaseResponse;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.ClaseRepository;
import reprogramacion_api.support.Datos;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaseServiceTest {

    @Mock
    private ClaseRepository claseRepo;
    @InjectMocks
    private ClaseService service;

    @Test
    void listarDelDocente_conClases_mapeaALaRespuesta() {
        Usuario docente = Datos.usuario(2L, Rol.DOCENTE, "docente@utp.edu.pe");
        Curso curso = Datos.curso(10L, docente);
        Clase clase = Datos.clase(100L, curso);
        when(claseRepo.findByCursoDocenteCorreoOrderByFechaOriginalAsc("docente@utp.edu.pe"))
                .thenReturn(List.of(clase));

        List<ClaseResponse> res = service.listarDelDocente("docente@utp.edu.pe");

        assertEquals(1, res.size());
        assertEquals(100L, res.get(0).id().longValue());
        assertEquals("Desarrollo Web", res.get(0).curso());
        assertEquals("Spring Boot", res.get(0).tema());
    }

    @Test
    void listarDelDocente_sinClases_devuelveListaVacia() {
        when(claseRepo.findByCursoDocenteCorreoOrderByFechaOriginalAsc("nadie@utp.edu.pe"))
                .thenReturn(List.of());

        assertTrue(service.listarDelDocente("nadie@utp.edu.pe").isEmpty());
    }
}
