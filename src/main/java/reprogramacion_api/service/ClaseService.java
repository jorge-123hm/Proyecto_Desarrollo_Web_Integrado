package reprogramacion_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reprogramacion_api.dto.response.ClaseResponse;
import reprogramacion_api.repository.ClaseRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaseService {

    private final ClaseRepository claseRepo;

    @Transactional(readOnly = true)
    public List<ClaseResponse> listarDelDocente(String correo) {
        return claseRepo.findByCursoDocenteCorreoOrderByFechaOriginalAsc(correo).stream()
                .map(ClaseResponse::from)
                .toList();
    }
}