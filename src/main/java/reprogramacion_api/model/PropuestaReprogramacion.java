package reprogramacion_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "propuestas_reprogramacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropuestaReprogramacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @Column(nullable = false)
    private LocalDate fechaPropuesta;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    /** alumnos libres / total de alumnos * 100 */
    @Column(nullable = false)
    private Double porcentajeCoincidencia;

    @Column(nullable = false)
    private Integer alumnosConConflicto;

    /** true si el docente la ingresó manualmente */
    @Builder.Default
    @Column(nullable = false)
    private boolean manual = false;
}
