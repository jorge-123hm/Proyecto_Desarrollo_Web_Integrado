package reprogramacion_api.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import reprogramacion_api.model.BloqueNoDisponible;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.Matricula;
import reprogramacion_api.model.PropuestaReprogramacion;
import reprogramacion_api.model.SolicitudReprogramacion;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.EstadoSolicitud;
import reprogramacion_api.model.enums.Rol;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Fábrica de datos de prueba compartida por todas las pruebas unitarias. */
public final class Datos {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private Datos() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static LocalDate futuro() {
        return hoy().plusDays(10);
    }

    public static LocalDate pasado() {
        return hoy().minusDays(1);
    }

    public static Usuario usuario(Long id, Rol rol, String correo) {
        return Usuario.builder()
                .id(id).cui("U" + id).nombres("Nombre" + id).apellidos("Apellido" + id)
                .correo(correo).password("hash").rol(rol).activo(true).build();
    }

    public static Curso curso(Long id, Usuario docente) {
        return Curso.builder()
                .id(id).nombre("Desarrollo Web").seccion("A").periodo("2026-II")
                .docente(docente).build();
    }

    public static Clase clase(Long id, Curso curso) {
        return Clase.builder()
                .id(id).curso(curso).fechaOriginal(hoy().plusDays(3))
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(10, 0))
                .tema("Spring Boot").build();
    }

    public static PropuestaReprogramacion propuesta(Long id, Clase clase, LocalDate fecha) {
        return PropuestaReprogramacion.builder()
                .id(id).clase(clase).fechaPropuesta(fecha)
                .horaInicio(LocalTime.of(14, 0)).horaFin(LocalTime.of(16, 0))
                .porcentajeCoincidencia(75.0).alumnosConConflicto(1).manual(false).build();
    }

    public static SolicitudReprogramacion solicitud(Long id, PropuestaReprogramacion propuesta,
                                                    Usuario docente, EstadoSolicitud estado) {
        return SolicitudReprogramacion.builder()
                .id(id).propuesta(propuesta).docente(docente).estado(estado).build();
    }

    public static BloqueNoDisponible bloque(Usuario usuario, DayOfWeek dia, int desde, int hasta) {
        return BloqueNoDisponible.builder()
                .id(1L).usuario(usuario).diaSemana(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0))
                .motivo("Motivo").build();
    }

    public static Matricula matricula(Usuario alumno, Curso curso) {
        return Matricula.builder().alumno(alumno).curso(curso).build();
    }

    /** Authentication ya autenticado: getName() devuelve el correo. */
    public static Authentication auth(String correo, String... roles) {
        String[] conPrefijo = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            conPrefijo[i] = "ROLE_" + roles[i];
        }
        return new UsernamePasswordAuthenticationToken(correo, null,
                AuthorityUtils.createAuthorityList(conPrefijo));
    }
}
