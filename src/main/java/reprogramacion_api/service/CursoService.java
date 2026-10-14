package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepo;
    private final UsuarioRepository usuarioRepo;
    private final MatriculaRepository matriculaRepo;

    // ---------- CRUD de cursos ----------

    @Transactional(readOnly = true)
    public List<CursoResponse> listar(Long docenteId) {
        List<Curso> lista = (docenteId == null) ? cursoRepo.findAll() : cursoRepo.findByDocenteId(docenteId);
        return lista.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CursoResponse obtener(Long id) {
        return toResponse(buscarCurso(id));
    }

    @Transactional
    public CursoResponse crear(CursoRequest req) {
        Curso c = Curso.builder()
                .nombre(req.nombre())
                .seccion(req.seccion())
                .periodo(req.periodo())
                .docente(buscarDocente(req.docenteId()))
                .build();
        return toResponse(cursoRepo.save(c));
    }

    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest req) {
        Curso c = buscarCurso(id);
        c.setNombre(req.nombre());
        c.setSeccion(req.seccion());
        c.setPeriodo(req.periodo());
        c.setDocente(buscarDocente(req.docenteId()));
        return toResponse(cursoRepo.save(c));
    }

    @Transactional
    public void eliminar(Long id) {
        Curso c = buscarCurso(id);
        matriculaRepo.deleteByCursoId(id);   // primero las matrículas del curso
        cursoRepo.delete(c);
        cursoRepo.flush();                   // si tiene clases asociadas, falla con 409
    }

    // ---------- Matrículas ----------

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarAlumnos(Long cursoId) {
        buscarCurso(cursoId);
        return matriculaRepo.findByCursoId(cursoId).stream()
                .map(m -> UsuarioResponse.from(m.getAlumno()))
                .toList();
    }

    @Transactional
    public void matricular(Long cursoId, Long alumnoId) {
        Curso curso = buscarCurso(cursoId);
        Usuario alumno = usuarioRepo.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado: " + alumnoId));

        if (alumno.getRol() != Rol.ALUMNO) {
            throw new ReglaNegocioException("El usuario indicado no tiene rol ALUMNO");
        }
        if (matriculaRepo.existsByAlumnoIdAndCursoId(alumnoId, cursoId)) {
            throw new ReglaNegocioException("El alumno ya está matriculado en este curso");
        }

        matriculaRepo.save(Matricula.builder().alumno(alumno).curso(curso).build());
    }

    @Transactional
    public void desmatricular(Long cursoId, Long alumnoId) {
        if (!matriculaRepo.existsByAlumnoIdAndCursoId(alumnoId, cursoId)) {
            throw new RecursoNoEncontradoException("El alumno no está matriculado en este curso");
        }
        matriculaRepo.deleteByAlumnoIdAndCursoId(alumnoId, cursoId);
    }

    // ---------- Auxiliares ----------

    private Curso buscarCurso(Long id) {
        return cursoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado: " + id));
    }

    private Usuario buscarDocente(Long id) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado: " + id));
        if (u.getRol() != Rol.DOCENTE) {
            throw new ReglaNegocioException("El usuario indicado no tiene rol DOCENTE");
        }
        return u;
    }

    private CursoResponse toResponse(Curso c) {
        Usuario d = c.getDocente();
        return new CursoResponse(c.getId(), c.getNombre(), c.getSeccion(), c.getPeriodo(),
                d.getId(), d.getNombres() + " " + d.getApellidos());
    }
}