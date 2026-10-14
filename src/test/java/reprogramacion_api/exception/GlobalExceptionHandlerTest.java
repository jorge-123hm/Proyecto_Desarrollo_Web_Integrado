package reprogramacion_api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void credencialesIncorrectas_devuelve401() {
        ResponseEntity<Map<String, String>> res = handler.credenciales(new BadCredentialsException("mal"));

        assertEquals(401, res.getStatusCode().value());
        assertEquals("Credenciales inválidas", res.getBody().get("error"));
    }

    @Test
    void usuarioDeshabilitado_devuelve401() {
        ResponseEntity<Map<String, String>> res = handler.credenciales(new DisabledException("inactivo"));

        assertEquals(401, res.getStatusCode().value());
        assertEquals("Credenciales inválidas", res.getBody().get("error"));
    }

    @Test
    void validacion_devuelve400ConUnMensajePorCampo() {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "usuarioRequest");
        br.addError(new FieldError("usuarioRequest", "correo", "debe ser un correo válido"));
        br.addError(new FieldError("usuarioRequest", "correo", "no debe estar vacío"));
        br.addError(new FieldError("usuarioRequest", "nombres", "no debe estar vacío"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(br);

        ResponseEntity<Map<String, String>> res = handler.validacion(ex);

        assertEquals(400, res.getStatusCode().value());
        Map<String, String> body = res.getBody();
        assertEquals(2, body.size());
        assertEquals("debe ser un correo válido", body.get("correo"));
        assertEquals("no debe estar vacío", body.get("nombres"));
    }

    @Test
    void recursoNoEncontrado_devuelve404ConElMensaje() {
        ResponseEntity<Map<String, String>> res =
                handler.noEncontrado(new RecursoNoEncontradoException("Curso no encontrado: 9"));

        assertEquals(404, res.getStatusCode().value());
        assertEquals("Curso no encontrado: 9", res.getBody().get("error"));
    }

    @Test
    void reglaDeNegocio_devuelve400ConElMensaje() {
        ResponseEntity<Map<String, String>> res =
                handler.reglaNegocio(new ReglaNegocioException("Regla incumplida"));

        assertEquals(400, res.getStatusCode().value());
        assertEquals("Regla incumplida", res.getBody().get("error"));
    }

    @Test
    void integridadDeDatos_devuelve409() {
        ResponseEntity<Map<String, String>> res =
                handler.integridad(new DataIntegrityViolationException("fk"));

        assertEquals(409, res.getStatusCode().value());
        assertTrue(res.getBody().get("error").contains("restricción"));
    }

    @Test
    void excepcionesPropias_conservanSuMensaje() {
        assertEquals("a", new ReglaNegocioException("a").getMessage());
        assertEquals("b", new RecursoNoEncontradoException("b").getMessage());
    }
}
