package reprogramacion_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reprogramacion_api.dto.response.NotificacionResponse;
import reprogramacion_api.service.NotificacionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService service;

    @GetMapping
    public List<NotificacionResponse> listar(Authentication auth) {
        return service.listar(auth.getName());
    }

    /** Para el globito de pendientes en la barra superior de Angular */
    @GetMapping("/no-leidas/cantidad")
    public Map<String, Long> cantidadNoLeidas(Authentication auth) {
        return service.contarNoLeidas(auth.getName());
    }

    @PutMapping("/{id}/leer")
    public NotificacionResponse marcarLeida(Authentication auth, @PathVariable Long id) {
        return service.marcarLeida(auth.getName(), id);
    }
}