package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.CalculoCaqDtos.CalculoCaqDetalheDto;
import br.com.caqi.engine.api.dto.CalculoCaqDtos.CalculoCaqResumoDto;
import br.com.caqi.engine.api.dto.InsumoDtos.InsumoDto;
import br.com.caqi.engine.core.exception.NotFoundException;
import br.com.caqi.engine.domain.repo.CalculoCaqRepository;
import br.com.caqi.engine.domain.repo.InsumoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Endpoints públicos LAI — cálculos CAQ/CAQi por escola e catálogo de insumos.
 * Cache 5 min. Sem auth.
 */
@RestController
@RequestMapping("/api/public/transparencia")
@RequiredArgsConstructor
@Tag(name = "transparencia-publica",
        description = "Dados públicos do motor CAQ — cálculos por escola/etapa e catálogo de insumos")
public class PublicTransparenciaController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final CalculoCaqRepository calculoRepo;
    private final InsumoRepository insumoRepo;

    @Operation(summary = "Lista todos os cálculos CAQ/CAQi persistidos (resumo, sem memória)")
    @GetMapping("/calculos")
    public ResponseEntity<List<CalculoCaqResumoDto>> calculos() {
        List<CalculoCaqResumoDto> body = calculoRepo.findAll().stream()
                .map(CalculoCaqResumoDto::from).toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }

    @Operation(summary = "Detalhe de um cálculo com memória item-a-item")
    @GetMapping("/calculos/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<CalculoCaqDetalheDto> calculo(@PathVariable Long id) {
        CalculoCaqDetalheDto body = calculoRepo.findById(id)
                .map(CalculoCaqDetalheDto::from)
                .orElseThrow(() -> new NotFoundException("Calculo", id));
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }

    @Operation(summary = "Catálogo público de insumos (sem custos confidenciais — só estrutura)")
    @GetMapping("/insumos")
    public ResponseEntity<List<InsumoDto>> insumos() {
        List<InsumoDto> body = insumoRepo.findAll().stream().map(InsumoDto::from).toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }
}
