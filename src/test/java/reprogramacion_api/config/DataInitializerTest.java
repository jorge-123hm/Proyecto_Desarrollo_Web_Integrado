package reprogramacion_api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import reprogramacion_api.model.BloqueNoDisponible;
import reprogramacion_api.model.Clase;
import reprogramacion_api.model.Curso;
import reprogramacion_api.model.Matricula;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.model.enums.Rol;
import reprogramacion_api.repository.BloqueNoDisponibleRepository;
import reprogramacion_api.repository.ClaseRepository;
import reprogramacion_api.repository.CursoRepository;
import reprogramacion_api.repository.MatriculaRepository;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.support.Datos;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerTest {

    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private MatriculaRepository matriculaRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private BloqueNoDisponibleRepository bloqueRepo;
    @Mock
    private PasswordEncoder encoder;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(usuarioRepo, cursoRepo, matriculaRepo, claseRepo, bloqueRepo, encoder);
        when(encoder.encode(any())).thenReturn("HASH");
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void baseVacia_creaUsuariosCursoMatriculasClaseYBloques() throws Exception {
        when(usuarioRepo.findByCorreo(any())).thenReturn(Optional.empty());
        when(cursoRepo.count()).thenReturn(0L);

        CommandLineRunner runner = initializer.seedDatos();
        runner.run();

        verify(usuarioRepo, times(6)).save(any(Usuario.class));
        verify(cursoRepo).save(any(Curso.class));
        verify(matriculaRepo, times(4)).save(any(Matricula.class));
        verify(claseRepo).save(any(Clase.class));
        verify(bloqueRepo, times(8)).save(any(BloqueNoDisponible.class));
    }

    @Test
    void baseYaSembrada_noDuplicaNada() throws Exception {
        Usuario existente = Datos.usuario(1L, Rol.ALUMNO, "x@utp.edu.pe");
        when(usuarioRepo.findByCorreo(any())).thenReturn(Optional.of(existente));
        when(cursoRepo.count()).thenReturn(1L);

        initializer.seedDatos().run();

        verify(usuarioRepo, never()).save(any(Usuario.class));
        verify(cursoRepo, never()).save(any(Curso.class));
        verify(matriculaRepo, never()).save(any(Matricula.class));
        verify(claseRepo, never()).save(any(Clase.class));
        verify(bloqueRepo, never()).save(any(BloqueNoDisponible.class));
    }
}
