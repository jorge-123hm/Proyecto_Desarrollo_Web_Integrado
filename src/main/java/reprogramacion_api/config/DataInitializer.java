package reprogramacion_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import reprogramacion_api.model.*;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Datos de prueba SOLO para desarrollo. Clave de todos los usuarios: 123456 */
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UsuarioRepository usuarioRepo;
    private final CursoRepository cursoRepo;
    private final MatriculaRepository matriculaRepo;
    private final ClaseRepository claseRepo;
    private final BloqueNoDisponibleRepository bloqueRepo;
    private final PasswordEncoder encoder;

    @Bean
    CommandLineRunner seedDatos() {
        return args -> {
            usuario("U00000001", "Carlos", "Atencio Torres", "coordinacion@utp.edu.pe", Rol.COORDINACION);
            Usuario docente = usuario("U00000002", "Pedro", "Huaynalaya Montes", "docente@utp.edu.pe", Rol.DOCENTE);
            Usuario a1 = usuario("U00000003", "Luciana", "Canales Palomino", "alumno1@utp.edu.pe", Rol.ALUMNO);
            Usuario a2 = usuario("U00000004", "Reiner", "Paiva Ticona", "alumno2@utp.edu.pe", Rol.ALUMNO);
            Usuario a3 = usuario("U00000005", "Ana", "Ruiz Medina", "alumno3@utp.edu.pe", Rol.ALUMNO);
            Usuario a4 = usuario("U00000006", "Jorge", "Quispe Vela", "alumno4@utp.edu.pe", Rol.ALUMNO);

            if (cursoRepo.count() > 0) return;   // el escenario de prueba se crea una sola vez

            Curso curso = cursoRepo.save(Curso.builder()
                    .nombre("Desarrollo Web Integrado").seccion("A").periodo("2026-II")
                    .docente(docente).build());

            for (Usuario a : new Usuario[]{a1, a2, a3, a4}) {
                matriculaRepo.save(Matricula.builder().alumno(a).curso(curso).build());
            }

            claseRepo.save(Clase.builder()
                    .curso(curso)
                    .fechaOriginal(LocalDate.now().plusDays(3))
                    .horaInicio(LocalTime.of(8, 0))
                    .horaFin(LocalTime.of(10, 0))
                    .tema("Introducción a Spring Boot")
                    .build());

            // Docente
            bloque(docente, DayOfWeek.MONDAY, 14, 18, "Trabajo externo");
            bloque(docente, DayOfWeek.TUESDAY, 8, 10, "Otro curso");
            // Alumnos
            bloque(a1, DayOfWeek.MONDAY, 8, 10, "Cálculo II");
            bloque(a1, DayOfWeek.WEDNESDAY, 10, 12, "Física");
            bloque(a2, DayOfWeek.MONDAY, 9, 11, "Trabajo");
            bloque(a2, DayOfWeek.THURSDAY, 18, 22, "Trabajo");
            bloque(a3, DayOfWeek.TUESDAY, 10, 12, "Inglés");
            bloque(a4, DayOfWeek.MONDAY, 8, 12, "Prácticas");
        };
    }

    private Usuario usuario(String cui, String nombres, String apellidos, String correo, Rol rol) {
        return usuarioRepo.findByCorreo(correo).orElseGet(() -> usuarioRepo.save(Usuario.builder()
                .cui(cui).nombres(nombres).apellidos(apellidos)
                .correo(correo).password(encoder.encode("123456"))
                .rol(rol).build()));
    }

    private void bloque(Usuario u, DayOfWeek dia, int desde, int hasta, String motivo) {
        bloqueRepo.save(BloqueNoDisponible.builder()
                .usuario(u).diaSemana(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0))
                .motivo(motivo).build());
    }
}