package reprogramacion_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reprogramacion_api.dto.response.ClaseResponse;
import reprogramacion_api.service.ClaseService;
import reprogramacion_api.service.OpcionesHorarioService;
import reprogramacion_api.service.algoritmo.OpcionHorario;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clases")
@RequiredArgsConstructor
public class ClaseController {

    private final OpcionesHorarioService opcionesService;
    private final ClaseService claseService;

    /** Clases de los cursos que dicta el docente autenticado */
    @GetMapping("/mias")
    public List<ClaseResponse> mias(Authentication auth) {
        return claseService.listarDelDocente(auth.getName());
    }

    /** GET /api/clases/1/opciones?desde=2026-10-14&hasta=2026-10-28&limite=10 (todos opcionales) */
    @GetMapping("/{id}/opciones")
    public List<OpcionHorario> opciones(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite) {
        return opcionesService.calcular(auth.getName(), id, desde, hasta, limite);
    }
}