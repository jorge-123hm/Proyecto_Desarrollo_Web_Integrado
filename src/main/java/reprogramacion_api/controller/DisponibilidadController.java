package reprogramacion_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reprogramacion_api.dto.request.BloqueRequest;
import reprogramacion_api.dto.request.DisponibilidadRequest;
import reprogramacion_api.dto.response.BloqueResponse;
import reprogramacion_api.dto.response.DisponibilidadResumenResponse;
import reprogramacion_api.service.DisponibilidadService;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService service;

    /** Mis bloques de no disponibilidad */
    @GetMapping
    public List<BloqueResponse> listar(Authentication auth) {
        return service.listar(auth.getName());
    }

    /** Horas ocupadas y tope semanal (el tope solo aplica a alumnos) */
    @GetMapping("/resumen")
    public DisponibilidadResumenResponse resumen(Authentication auth) {
        return service.resumen(auth.getName());
    }

    /** Agrega un bloque */
    @PostMapping
    public ResponseEntity<BloqueResponse> agregar(Authentication auth,
            @Valid @RequestBody BloqueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregar(auth.getName(), req));
    }

    /** Reemplaza todo mi horario por la lista enviada */
    @PutMapping
    public List<BloqueResponse> reemplazar(Authentication auth,
            @Valid @RequestBody DisponibilidadRequest req) {
        return service.reemplazar(auth.getName(), req.bloques());
    }

    /** Elimina un bloque mío */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication auth, @PathVariable Long id) {
        service.eliminar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}