package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.CalculoCaqDtos.CalculoCaqDetalheDto;
import br.com.caqi.engine.api.dto.CalculoCaqDtos.CalculoCaqResumoDto;
import br.com.caqi.engine.core.exception.NotFoundException;
import br.com.caqi.engine.domain.repo.CalculoCaqRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/caqi/calculos")
@RequiredArgsConstructor
@Tag(name = "caqi-calculos", description = "Histórico de cálculos persistidos (POST está em /api/v1/caqi/calculos via CaqController)")
public class CalculosController {

    private final CalculoCaqRepository calculoRepo;

    @Operation(summary = "Lista todos os cálculos persistidos (resumo)")
    @GetMapping
    public List<CalculoCaqResumoDto> listar() {
        return calculoRepo.findAll().stream()
                .map(CalculoCaqResumoDto::from)
                .toList();
    }

    @Operation(summary = "Busca cálculo por id, com memória completa item-a-item")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)  // mantém sessão aberta para LAZY load dos itens
    public CalculoCaqDetalheDto buscar(@PathVariable Long id) {
        return calculoRepo.findById(id)
                .map(CalculoCaqDetalheDto::from)
                .orElseThrow(() -> new NotFoundException("Calculo", id));
    }
}
