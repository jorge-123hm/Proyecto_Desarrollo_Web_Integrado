package reprogramacion_api.model;

import jakarta.persistence.*;
import lombok.*;
import reprogramacion_api.model.enums.EstadoSolicitud;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_reprogramacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudReprogramacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Una propuesta origina como máximo una solicitud */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propuesta_id", nullable = false, unique = true)
    private PropuestaReprogramacion propuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Usuario docente;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(length = 500)
    private String observacion;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    private LocalDateTime fechaResolucion;

    /** Usuario de coordinación que aprobó o rechazó */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por_id")
    private Usuario resueltoPor;
}
