package reprogramacion_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Bloque semanal recurrente en el que un usuario NO está disponible
 * (clases en otros cursos, trabajo, compromisos externos, etc.).
 */
@Entity
@Table(name = "bloques_no_disponibles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BloqueNoDisponible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(length = 200)
    private String motivo;
}
