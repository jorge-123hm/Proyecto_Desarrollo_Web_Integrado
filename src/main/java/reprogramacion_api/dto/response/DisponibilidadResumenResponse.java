package reprogramacion_api.dto.response;

/**
 * Resumen del horario de un usuario: horas marcadas como ocupadas frente al tope semanal.
 * Si {@code limiteHoras} es null, el usuario no tiene tope (docentes y coordinación).
 */
public record DisponibilidadResumenResponse(
        double horasOcupadas,
        Integer limiteHoras,
        Double horasRestantes,
        boolean aplicaLimite) {

    public static DisponibilidadResumenResponse de(long minutosOcupados, Integer limiteHoras) {
        double ocupadas = horas(minutosOcupados);
        Double restantes = limiteHoras == null ? null : Math.max(0, limiteHoras - ocupadas);
        return new DisponibilidadResumenResponse(ocupadas, limiteHoras, restantes, limiteHoras != null);
    }

    /** Minutos a horas con 2 decimales */
    public static double horas(long minutos) {
        return Math.round(minutos / 60.0 * 100.0) / 100.0;
    }
}
