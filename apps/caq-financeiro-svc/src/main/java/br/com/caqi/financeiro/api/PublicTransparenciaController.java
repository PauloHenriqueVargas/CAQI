package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.ContratoDtos.ContratoDto;
import br.com.caqi.financeiro.api.dto.DespesaDtos.DespesaDto;
import br.com.caqi.financeiro.api.dto.SiopeDtos.SiopeExportDto;
import br.com.caqi.financeiro.domain.FundebService;
import br.com.caqi.financeiro.domain.SiopeService;
import br.com.caqi.financeiro.domain.repo.ContratoRepository;
import br.com.caqi.financeiro.domain.repo.DespesaRepository;
import br.com.caqi.shared.dto.ExecucaoFundebDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.time.Duration;
import java.util.List;

/**
 * Endpoints públicos de transparência (LAI — Lei 12.527/2011, LRF art. 48-A).
 * SEM autenticação. Cache de 5 min para reduzir carga em portal público.
 *
 * NÃO expõe dados pessoais (LGPD): folha individual, CPF de servidor, dados
 * pessoais de aluno/responsável. Expõe APENAS dados públicos por natureza
 * (CNPJ de fornecedor PJ, valores agregados, contratos firmados).
 */
@RestController
@RequestMapping("/api/public/transparencia")
@RequiredArgsConstructor
@Tag(name = "transparencia-publica",
        description = "Dados públicos para transparência ativa LAI/LRF — sem autenticação, cacheados 5 min")
public class PublicTransparenciaController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final ContratoRepository contratoRepo;
    private final DespesaRepository despesaRepo;
    private final FundebService fundebService;
    private final SiopeService siopeService;

    @Operation(summary = "Lista contratos públicos (Lei 14.133/2021)")
    @GetMapping("/contratos")
    public ResponseEntity<List<ContratoDto>> contratos() {
        List<ContratoDto> body = contratoRepo.findAll().stream().map(ContratoDto::from).toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }

    @Operation(summary = "Lista despesas do ano (sem dados pessoais)")
    @GetMapping("/despesas")
    public ResponseEntity<List<DespesaDto>> despesas(@RequestParam(required = false) Integer ano) {
        int alvo = (ano != null) ? ano : Year.now().getValue();
        String inicio = String.format("%04d01", alvo);
        String fim = String.format("%04d12", alvo);
        List<DespesaDto> body = despesaRepo.listarDoAno(inicio, fim).stream().map(DespesaDto::from).toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }

    @Operation(summary = "Execução Fundeb/MDE/VAAT do ano (% e valores agregados)")
    @GetMapping("/fundeb-execucao")
    public ResponseEntity<ExecucaoFundebDto> fundebExecucao(@RequestParam(required = false) Integer ano) {
        int alvo = (ano != null) ? ano : Year.now().getValue();
        return ResponseEntity.ok().cacheControl(CACHE).body(fundebService.calcular(alvo));
    }

    @Operation(summary = "Quadro SIOPE consolidado — versão pública (omite pendências internas)")
    @GetMapping("/siope-quadro")
    public ResponseEntity<SiopeExportDto> siopeQuadro(@RequestParam(required = false) Integer ano) {
        int alvo = (ano != null) ? ano : Year.now().getValue();
        SiopeExportDto full = siopeService.gerar(alvo);
        // Versão pública: zera lista de pendências (essas são para auditoria interna)
        SiopeExportDto publico = new SiopeExportDto(
                full.ano(), full.tenantMunicipioId(), full.geradoEm(),
                full.receitas(), full.despesas(), full.vinculacoes(), List.of());
        return ResponseEntity.ok().cacheControl(CACHE).body(publico);
    }
}
