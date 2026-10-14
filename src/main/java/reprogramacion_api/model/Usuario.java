package reprogramacion_api.model;

import jakarta.persistence.*;
import lombok.*;
import reprogramacion_api.model.enums.Rol;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código universitario (CUI) */
    @Column(nullable = false, unique = true, length = 20)
    private String cui;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    /** Se usa como nombre de usuario para iniciar sesión */
    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    /** Contraseña cifrada con BCrypt */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
