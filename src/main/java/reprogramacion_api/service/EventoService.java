package reprogramacion_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class EventoService {

    /** correo -> conexiones abiertas (un usuario puede tener varias pestañas) */
    private final Map<String, List<SseEmitter>> emisores = new ConcurrentHashMap<>();

    public SseEmitter suscribir(String correo) {
        SseEmitter emisor = new SseEmitter(0L);
        emisores.computeIfAbsent(correo, k -> new CopyOnWriteArrayList<>()).add(emisor);

        emisor.onCompletion(() -> quitar(correo, emisor));
        emisor.onTimeout(() -> quitar(correo, emisor));
        emisor.onError(t -> quitar(correo, emisor));

        enviar(correo, emisor, "conectado", Map.of("ok", true));
        return emisor;
    }

    /** Envía un evento a todas las pestañas de esos correos (espera al commit si hay transacción) */
    public void publicar(Collection<String> correos, String tipo, Map<String, Object> datos) {
        Set<String> destino = new HashSet<>(correos);
        if (destino.isEmpty()) return;

        Runnable envio = () -> destino.forEach(c -> emitirA(c, tipo, datos));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    @Scheduled(fixedRate = 20_000)
    void latido() {
        emisores.forEach((correo, lista) -> lista.forEach(e -> {
            try {
                synchronized (e) {
                    e.send(SseEmitter.event().comment("ping"));
                }
            } catch (IOException | IllegalStateException ex) {
                quitar(correo, e);
            }
        }));
    }

    private void emitirA(String correo, String tipo, Map<String, Object> datos) {
        List<SseEmitter> lista = emisores.get(correo);
        if (lista == null) return;
        lista.forEach(e -> enviar(correo, e, tipo, datos));
    }

    private void enviar(String correo, SseEmitter emisor, String tipo, Object datos) {
        try {
            synchronized (emisor) {
                emisor.send(SseEmitter.event().name(tipo).data(datos, MediaType.APPLICATION_JSON));
            }
        } catch (IOException | IllegalStateException ex) {
            log.debug("Conexión SSE cerrada para {}: {}", correo, ex.getMessage());
            quitar(correo, emisor);
        }
    }

    private void quitar(String correo, SseEmitter emisor) {
        emisores.computeIfPresent(correo, (k, lista) -> {
            lista.remove(emisor);
            return lista.isEmpty() ? null : lista;
        });
    }
}