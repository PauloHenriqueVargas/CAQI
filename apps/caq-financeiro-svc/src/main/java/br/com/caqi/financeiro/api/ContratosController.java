package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.ContratoDtos.ContratoCreateDto;
import br.com.caqi.financeiro.api.dto.ContratoDtos.ContratoDto;
import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoComRetencoesDto;
import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoCreateDto;
import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoDto;
import br.com.caqi.financeiro.domain.MedicaoComRetencaoService;
import br.com.caqi.financeiro.domain.entity.Contrato;
import br.com.caqi.financeiro.domain.repo.ContratoRepository;
import br.com.caqi.financeiro.domain.repo.MedicaoContratoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/financeiro/contratos")
@RequiredArgsConstructor
@Tag(name = "financeiro-contratos",
        description = "Contratos (Lei 14.133/2021): PREGAO_ELETRONICO, CONCORRENCIA, DISPENSA, INEXIGIBILIDADE, DIALOGO_COMPETITIVO, CONCURSO, LEILAO")
public class ContratosController {

    private final ContratoRepository contratoRepo;
    private final MedicaoContratoRepository medicaoRepo;
    private final MedicaoComRetencaoService medicaoService;

    @GetMapping
    public List<ContratoDto> listar(@RequestParam(required = false) Long fornecedorId) {
        if (fornecedorId != null) {
            return contratoRepo.findByFornecedorIdOrderByDataAssinaturaDesc(fornecedorId)
                    .stream().map(ContratoDto::from).toList();
        }
        return contratoRepo.findAll().stream().map(ContratoDto::from).toList();
    }

    @GetMapping("/{id}")
    public ContratoDto buscar(@PathVariable Long id) {
        return ContratoDto.from(getOrThrow(id));
    }

    @Operation(summary = "Cria contrato (modalidades da Lei 14.133)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ContratoDto criar(@Valid @RequestBody ContratoCreateDto dto) {
        return ContratoDto.from(contratoRepo.save(dto.toEntity()));
    }

    // ── Medições ────────────────────────────────────────

    @GetMapping("/{contratoId}/medicoes")
    public List<MedicaoDto> listarMedicoes(@PathVariable Long contratoId) {
        getOrThrow(contratoId);
        return medicaoRepo.findByContratoIdOrderByCompetenciaDesc(contratoId)
                .stream().map(MedicaoDto::from).toList();
    }

    @Operation(summary = "Registra medição contratual. Quando tipoServico+aliquotaIssMunicipal informados, " +
            "devolve preview de retenções para emissão da guia (não persiste retenção).")
    @PostMapping("/{contratoId}/medicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicaoComRetencoesDto registrarMedicao(@PathVariable Long contratoId,
                                                   @Valid @RequestBody MedicaoCreateDto dto) {
        return medicaoService.registrarMedicao(contratoId, dto);
    }

    private Contrato getOrThrow(Long id) {
        return contratoRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato " + id));
    }
}
