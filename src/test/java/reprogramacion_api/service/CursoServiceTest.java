package reprogramacion_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reprogramacion_api.dto.request.CursoRequest;
import reprogramacion_api.dto.response.CursoResponse;
import reprogramacion_api.dto.response.UsuarioResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.Matricula;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.CursoRepository;
import reprogramacion_api.repository.MatriculaRepository;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.support.Datos;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private MatriculaRepository matriculaRepo;
    @InjectMocks
    private CursoService service;

    private Usuario docente;
    private Usuario alumno;
    private Curso curso;

    @BeforeEach
    void setUp() {
        docente = Datos.usuario(2L, Rol.DOCENTE, "docente@utp.edu.pe");
        alumno = Datos.usuario(3L, Rol.ALUMNO, "alumno@utp.edu.pe");
        curso = Datos.curso(10L, docente);
    }

    private CursoRequest req() {
        return new CursoRequest("Base de Datos", "B", "2026-II", 2L);
    }

    // ---------- CRUD ----------

    @Test
    void listar_sinDocente_devuelveTodosLosCursos() {
        when(cursoRepo.findAll()).thenReturn(List.of(curso));

        List<CursoResponse> res = service.listar(null);

        assertEquals(1, res.size());
        assertEquals("Nombre2 Apellido2", res.get(0).docenteNombre());
        verify(cursoRepo, never()).findByDocenteId(any());
    }

    @Test
    void listar_conDocente_filtraPorDocente() {
        when(cursoRepo.findByDocenteId(2L)).thenReturn(List.of(curso));

        List<CursoResponse> res = service.listar(2L);

        assertEquals(1, res.size());
        assertEquals(2L, res.get(0).docenteId().longValue());
        verify(cursoRepo, never()).findAll();
    }

    @Test
    void obtener_existente_devuelveResponse() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));

        CursoResponse res = service.obtener(10L);

        assertEquals("Desarrollo Web", res.nombre());
        assertEquals("A", res.seccion());
        assertEquals("2026-II", res.periodo());
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(cursoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
    }

    @Test
    void crear_ok_guardaConElDocenteIndicado() {
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(docente));
        when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        CursoResponse res = service.crear(req());

        ArgumentCaptor<Curso> cap = ArgumentCaptor.forClass(Curso.class);
        verify(cursoRepo).save(cap.capture());
        assertEquals("Base de Datos", cap.getValue().getNombre());
        assertSame(docente, cap.getValue().getDocente());
        assertEquals("B", res.seccion());
    }

    @Test
    void crear_docenteInexistente_lanzaNoEncontrado() {
        when(usuarioRepo.findById(2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(req()));
        verify(cursoRepo, never()).save(any());
    }

    @Test
    void crear_usuarioSinRolDocente_lanzaReglaNegocio() {
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(alumno));

        assertThrows(ReglaNegocioException.class, () -> service.crear(req()));
        verify(cursoRepo, never()).save(any());
    }

    @Test
    void actualizar_ok_modificaLosCampos() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(docente));
        when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

        CursoResponse res = service.actualizar(10L, req());

        assertEquals("Base de Datos", curso.getNombre());
        assertEquals("B", curso.getSeccion());
        assertEquals("Base de Datos", res.nombre());
    }

    @Test
    void actualizar_cursoInexistente_lanzaNoEncontrado() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(10L, req()));
    }

    @Test
    void eliminar_ok_borraMatriculasPrimeroYLuegoElCurso() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));

        service.eliminar(10L);

        InOrder orden = inOrder(matriculaRepo, cursoRepo);
        orden.verify(matriculaRepo).deleteByCursoId(10L);
        orden.verify(cursoRepo).delete(curso);
        orden.verify(cursoRepo).flush();
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(10L));
        verify(matriculaRepo, never()).deleteByCursoId(any());
    }

    // ---------- Matrículas ----------

    @Test
    void listarAlumnos_ok_devuelveLosAlumnosMatriculados() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));
        when(matriculaRepo.findByCursoId(10L)).thenReturn(List.of(Datos.matricula(alumno, curso)));

        List<UsuarioResponse> res = service.listarAlumnos(10L);

        assertEquals(1, res.size());
        assertEquals("alumno@utp.edu.pe", res.get(0).correo());
    }

    @Test
    void listarAlumnos_cursoInexistente_lanzaNoEncontrado() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.listarAlumnos(10L));
    }

    @Test
    void matricular_ok_guardaLaMatricula() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));
        when(usuarioRepo.findById(3L)).thenReturn(Optional.of(alumno));
        when(matriculaRepo.existsByAlumnoIdAndCursoId(3L, 10L)).thenReturn(false);

        service.matricular(10L, 3L);

        ArgumentCaptor<Matricula> cap = ArgumentCaptor.forClass(Matricula.class);
        verify(matriculaRepo).save(cap.capture());
        assertSame(alumno, cap.getValue().getAlumno());
        assertSame(curso, cap.getValue().getCurso());
    }

    @Test
    void matricular_cursoInexistente_lanzaNoEncontrado() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.matricular(10L, 3L));
    }

    @Test
    void matricular_alumnoInexistente_lanzaNoEncontrado() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));
        when(usuarioRepo.findById(3L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.matricular(10L, 3L));
    }

    @Test
    void matricular_usuarioSinRolAlumno_lanzaReglaNegocio() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(docente));

        assertThrows(ReglaNegocioException.class, () -> service.matricular(10L, 2L));
        verify(matriculaRepo, never()).save(any());
    }

    @Test
    void matricular_yaMatriculado_lanzaReglaNegocio() {
        when(cursoRepo.findById(10L)).thenReturn(Optional.of(curso));
        when(usuarioRepo.findById(3L)).thenReturn(Optional.of(alumno));
        when(matriculaRepo.existsByAlumnoIdAndCursoId(3L, 10L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.matricular(10L, 3L));
        verify(matriculaRepo, never()).save(any());
    }

    @Test
    void desmatricular_ok_borraLaMatricula() {
        when(matriculaRepo.existsByAlumnoIdAndCursoId(3L, 10L)).thenReturn(true);

        service.desmatricular(10L, 3L);

        verify(matriculaRepo).deleteByAlumnoIdAndCursoId(3L, 10L);
    }

    @Test
    void desmatricular_sinMatricula_lanzaNoEncontrado() {
        when(matriculaRepo.existsByAlumnoIdAndCursoId(3L, 10L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.desmatricular(10L, 3L));
        verify(matriculaRepo, never()).deleteByAlumnoIdAndCursoId(any(), any());
    }
}
