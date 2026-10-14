package reprogramacion_api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import reprogramacion_api.dto.request.UsuarioRequest;
import reprogramacion_api.dto.response.UsuarioResponse;
import reprogramacion_api.exception.RecursoNoEncontradoException;
import reprogramacion_api.exception.ReglaNegocioException;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.support.Datos;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repo;
    @Mock
    private PasswordEncoder encoder;
    @InjectMocks
    private UsuarioService service;

    private static UsuarioRequest req(String password, Boolean activo) {
        return new UsuarioRequest("U100", "Ana", "Ruiz", "ana@utp.edu.pe", password, Rol.ALUMNO, activo);
    }

    private static Usuario existente() {
        return Datos.usuario(1L, Rol.ALUMNO, "viejo@utp.edu.pe");
    }

    // ---------- listar / obtener ----------

    @Test
    void listar_sinRol_devuelveTodos() {
        when(repo.findAll()).thenReturn(List.of(
                Datos.usuario(1L, Rol.ALUMNO, "a@utp.edu.pe"),
                Datos.usuario(2L, Rol.DOCENTE, "d@utp.edu.pe")));

        List<UsuarioResponse> res = service.listar(null);

        assertEquals(2, res.size());
        verify(repo, never()).findByRol(any());
    }

    @Test
    void listar_conRol_filtraPorRol() {
        when(repo.findByRol(Rol.DOCENTE)).thenReturn(List.of(Datos.usuario(2L, Rol.DOCENTE, "d@utp.edu.pe")));

        List<UsuarioResponse> res = service.listar(Rol.DOCENTE);

        assertEquals(1, res.size());
        assertEquals(Rol.DOCENTE, res.get(0).rol());
        verify(repo, never()).findAll();
    }

    @Test
    void obtener_existente_devuelveResponse() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente()));

        UsuarioResponse res = service.obtener(1L);

        assertEquals("viejo@utp.edu.pe", res.correo());
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
    }

    // ---------- crear ----------

    @Test
    void crear_passwordNula_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(req(null, null)));
        verify(repo, never()).save(any());
    }

    @Test
    void crear_passwordEnBlanco_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(req("   ", null)));
    }

    @Test
    void crear_correoDuplicado_lanzaReglaNegocio() {
        when(repo.existsByCorreo("ana@utp.edu.pe")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.crear(req("secreto", null)));
        verify(repo, never()).save(any());
    }

    @Test
    void crear_cuiDuplicado_lanzaReglaNegocio() {
        when(repo.existsByCorreo("ana@utp.edu.pe")).thenReturn(false);
        when(repo.existsByCui("U100")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.crear(req("secreto", null)));
        verify(repo, never()).save(any());
    }

    @Test
    void crear_ok_codificaPasswordYQuedaActivoPorDefecto() {
        when(encoder.encode("secreto")).thenReturn("HASH");
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse res = service.crear(req("secreto", null));

        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(cap.capture());
        assertEquals("HASH", cap.getValue().getPassword());
        assertEquals("ana@utp.edu.pe", res.correo());
        assertEquals(Rol.ALUMNO, res.rol());
        assertTrue(res.activo());
    }

    @Test
    void crear_conActivoFalse_respetaElValor() {
        when(encoder.encode("secreto")).thenReturn("HASH");
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse res = service.crear(req("secreto", false));

        assertFalse(res.activo());
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_inexistente_lanzaNoEncontrado() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(1L, req(null, null)));
    }

    @Test
    void actualizar_correoDeOtroUsuario_lanzaReglaNegocio() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente()));
        when(repo.existsByCorreoAndIdNot("ana@utp.edu.pe", 1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1L, req(null, null)));
        verify(repo, never()).save(any());
    }

    @Test
    void actualizar_cuiDeOtroUsuario_lanzaReglaNegocio() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente()));
        when(repo.existsByCuiAndIdNot("U100", 1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1L, req(null, null)));
        verify(repo, never()).save(any());
    }

    @Test
    void actualizar_conPasswordYActivo_actualizaTodo() {
        Usuario u = existente();
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(encoder.encode("nueva")).thenReturn("HASH2");
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse res = service.actualizar(1L,
                new UsuarioRequest("U100", "Ana", "Ruiz", "ana@utp.edu.pe", "nueva", Rol.DOCENTE, false));

        assertEquals("HASH2", u.getPassword());
        assertEquals(Rol.DOCENTE, u.getRol());
        assertFalse(u.isActivo());
        assertEquals("ana@utp.edu.pe", res.correo());
    }

    @Test
    void actualizar_sinPasswordNiActivo_noTocaEsosCampos() {
        Usuario u = existente();
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(1L, req(null, null));

        assertEquals("hash", u.getPassword());
        assertTrue(u.isActivo());
        verify(encoder, never()).encode(any());
    }

    @Test
    void actualizar_passwordEnBlanco_noCambiaLaClave() {
        Usuario u = existente();
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(1L, req("   ", null));

        assertEquals("hash", u.getPassword());
        verify(encoder, never()).encode(any());
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_existente_borraYHaceFlush() {
        Usuario u = existente();
        when(repo.findById(1L)).thenReturn(Optional.of(u));

        service.eliminar(1L);

        verify(repo).delete(u);
        verify(repo).flush();
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(1L));
        verify(repo, never()).delete(any());
    }
}
