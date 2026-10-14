package reprogramacion_api.dto.response;

public record CursoResponse(
        Long id,
        String nombre,
        String seccion,
        String periodo,
        Long docenteId,
        String docenteNombre) {
}