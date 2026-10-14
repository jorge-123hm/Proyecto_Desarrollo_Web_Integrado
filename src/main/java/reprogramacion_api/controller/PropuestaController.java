package reprogramacion_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reprogramacion_api.dto.request.PropuestaRequest;
import reprogramacion_api.dto.response.PropuestaResponse;
import reprogramacion_api.service.OpcionesHorarioService;
import reprogramacion_api.service.PropuestaService;
import reprogramacion_api.service.algoritmo.OpcionHorario;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/propuestas")
@RequiredArgsConstructor
public class PropuestaController {

    private final PropuestaService propuestaService;
    private final OpcionesHorarioService opcionesService;

    /**
     * Vista previa para la fecha manual (CU004 paso 5): cuántos alumnos tienen
     * cruce.
     * GET
     * /api/propuestas/evaluar?claseId=1&fecha=2026-10-20&horaInicio=08:00&horaFin=10:00
     */
    @GetMapping("/evaluar")
    public OpcionHorario evaluar(
            Authentication auth,
            @RequestParam Long claseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {
        return opcionesService.evaluar(auth.getName(), claseId, fecha, horaInicio, horaFin);
    }

    /** Guarda la propuesta (sugerida o manual) */
    @PostMapping
    public ResponseEntity<PropuestaResponse> crear(Authentication auth,
            @Valid @RequestBody PropuestaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propuestaService.crear(auth.getName(), req));
    }
}