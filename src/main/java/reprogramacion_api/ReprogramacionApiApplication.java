package reprogramacion_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // latido de las conexiones SSE (EventoService)
public class ReprogramacionApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReprogramacionApiApplication.class, args);
    }
}
