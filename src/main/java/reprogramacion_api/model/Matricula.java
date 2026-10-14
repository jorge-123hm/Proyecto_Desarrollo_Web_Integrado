package reprogramacion_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "matriculas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"alumno_id", "curso_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;
}
