package reprogramacion_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reprogramacion_api.dto.request.ResolverRequest;
import reprogramacion_api.dto.request.SolicitudRequest;
import reprogramacion_api.dto.response.SolicitudResponse;
import reprogramacion_api.model.enums.EstadoSolicitud;
import reprogramacion_api.service.SolicitudService;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService service;

    /** CU005: el docente envía una propuesta como solicitud formal */
    @PostMapping
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<SolicitudResponse> enviar(Authentication auth,
            @Valid @RequestBody SolicitudRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.enviar(auth.getName(), req.propuestaId()));
    }

    /**
     * CU007 (y lista de pendientes de CU006).
     * GET /api/solicitudes?estado=PENDIENTE&cursoId=1 (ambos filtros opcionales)
     * Docente ve solo las suyas; coordinación ve todas.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCENTE','COORDINACION')")
    public List<SolicitudResponse> historial(Authentication auth,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Long cursoId) {
        return service.historial(auth.getName(), esCoordinacion(auth), estado, cursoId);
    }

    /** Atajo del docente: todas mis solicitudes */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('DOCENTE')")
    public List<SolicitudResponse> mias(Authentication auth) {
        return service.historial(auth.getName(), false, null, null);
    }

    /** Detalle de una solicitud */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCENTE','COORDINACION')")
    public SolicitudResponse detalle(Authentication auth, @PathVariable Long id) {
        return service.obtener(auth.getName(), esCoordinacion(auth), id);
    }

    /** CU006: coordinación aprueba o rechaza */
    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasRole('COORDINACION')")
    public SolicitudResponse resolver(Authentication auth, @PathVariable Long id,
            @Valid @RequestBody ResolverRequest req) {
        return service.resolver(auth.getName(), id, req.decision(), req.observacion());
    }

    private boolean esCoordinacion(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COORDINACION"));
    }
}