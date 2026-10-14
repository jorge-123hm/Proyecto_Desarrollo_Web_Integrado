package reprogramacion_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reprogramacion_api.dto.request.CursoRequest;
import reprogramacion_api.dto.response.CursoResponse;
import reprogramacion_api.dto.response.UsuarioResponse;
import reprogramacion_api.service.CursoService;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService service;

    /** GET /api/cursos  o  GET /api/cursos?docenteId=2 */
    @GetMapping
    public List<CursoResponse> listar(@RequestParam(required = false) Long docenteId) {
        return service.listar(docenteId);
    }

    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Matrículas -----

    @GetMapping("/{id}/alumnos")
    public List<UsuarioResponse> listarAlumnos(@PathVariable Long id) {
        return service.listarAlumnos(id);
    }

    @PostMapping("/{id}/alumnos/{alumnoId}")
    public ResponseEntity<Void> matricular(@PathVariable Long id, @PathVariable Long alumnoId) {
        service.matricular(id, alumnoId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/alumnos/{alumnoId}")
    public ResponseEntity<Void> desmatricular(@PathVariable Long id, @PathVariable Long alumnoId) {
        service.desmatricular(id, alumnoId);
        return ResponseEntity.noContent().build();
    }
}