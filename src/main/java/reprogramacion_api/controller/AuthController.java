package reprogramacion_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reprogramacion_api.dto.request.LoginRequest;
import reprogramacion_api.dto.response.LoginResponse;
import reprogramacion_api.model.Usuario;
import reprogramacion_api.repository.UsuarioRepository;
import reprogramacion_api.security.JwtService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        // Lanza BadCredentialsException si el usuario o la clave son incorrectos
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.correo(), req.password()));

        Usuario u = usuarioRepository.findByCorreo(req.correo()).orElseThrow();
        String token = jwtService.generateToken(u.getCorreo(), u.getRol());

        return ResponseEntity.ok(new LoginResponse(
                token, "Bearer", u.getId(), u.getNombres(), u.getCorreo(), u.getRol()));
    }
}
