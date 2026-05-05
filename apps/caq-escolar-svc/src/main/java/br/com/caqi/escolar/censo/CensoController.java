package br.com.caqi.escolar.censo;

import br.com.caqi.escolar.censo.dto.CensoImportResultDto;
import br.com.caqi.escolar.censo.dto.CensoImportacaoDto;
import br.com.caqi.escolar.domain.repo.CensoImportacaoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/v1/escolar/censo")
@RequiredArgsConstructor
@Tag(name = "Censo Escolar", description = "Importação de microdados Censo Escolar/INEP")
public class CensoController {

    private final CensoImportService importService;
    private final CensoMatriculaImportService matriculaImportService;
    private final CensoImportacaoRepository importacaoRepo;

    @Operation(summary = "Importa ESCOLAS_*.csv do Censo INEP",
               description = "Upload Latin-1 ; -separated. Filtra por município (cod IBGE 7-dig) " +
                             "e dependência municipal. Idempotente por (ano + hash + município).")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CensoImportResultDto> importar(
            @Parameter(description = "Arquivo ESCOLAS_*.csv do Censo INEP", required = true)
            @RequestParam("arquivo") MultipartFile arquivo,
            @Parameter(description = "Ano do Censo (default = ano atual)")
            @RequestParam(value = "anoCenso", required = false) Integer anoCenso,
            @AuthenticationPrincipal UserDetails user
    ) throws IOException {
        return ResponseEntity.ok(executar(arquivo, anoCenso, false, user));
    }

    @Operation(summary = "Dry-run: parse + filtro sem persistir",
               description = "Útil para validar o arquivo antes de aplicar. Retorna mesmas " +
                             "estatísticas que /import mas não modifica o BD.")
    @PostMapping(value = "/dry-run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CensoImportResultDto> dryRun(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "anoCenso", required = false) Integer anoCenso,
            @AuthenticationPrincipal UserDetails user
    ) throws IOException {
        return ResponseEntity.ok(executar(arquivo, anoCenso, true, user));
    }

    @Operation(summary = "Importa MATRICULA_*.csv (microdados aluno-level)",
               description = "Upload do microdado MATRICULA do INEP. Pseudonimiza ID_ALUNO via " +
                             "SHA-256 com salt do tenant (LGPD). Filtra por município + dependência municipal " +
                             "+ etapa mapeável. Streaming + batch insert (1000/commit) — suporta arquivos grandes.")
    @PostMapping(value = "/import-matriculas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CensoImportResultDto> importarMatriculas(
            @Parameter(description = "Arquivo MATRICULA_*.csv do Censo INEP", required = true)
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "anoCenso", required = false) Integer anoCenso,
            @AuthenticationPrincipal UserDetails user
    ) throws IOException {
        return ResponseEntity.ok(executarMatriculas(arquivo, anoCenso, false, user));
    }

    @Operation(summary = "Dry-run de MATRICULA_*.csv (parse + pseudonimização sem persistir)")
    @PostMapping(value = "/dry-run-matriculas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CensoImportResultDto> dryRunMatriculas(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "anoCenso", required = false) Integer anoCenso,
            @AuthenticationPrincipal UserDetails user
    ) throws IOException {
        return ResponseEntity.ok(executarMatriculas(arquivo, anoCenso, true, user));
    }

    @Operation(summary = "Lista importações Censo (mais recentes primeiro)")
    @GetMapping("/importacoes")
    public List<CensoImportacaoDto> listar(@RequestParam(required = false) Integer anoCenso) {
        var lista = (anoCenso != null)
                ? importacaoRepo.findByAnoCensoOrderByCriadoEmDesc(anoCenso)
                : importacaoRepo.findAllByOrderByCriadoEmDesc();
        return lista.stream().map(CensoImportacaoDto::from).toList();
    }

    @Operation(summary = "Detalhe de uma importação específica")
    @GetMapping("/importacoes/{id}")
    public ResponseEntity<CensoImportacaoDto> detalhe(@PathVariable Long id) {
        return importacaoRepo.findById(id)
                .map(CensoImportacaoDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CensoImportResultDto executar(MultipartFile arquivo, Integer anoCenso,
                                           boolean dryRun, UserDetails user) throws IOException {
        var p = preExec(arquivo, anoCenso, user, "ESCOLAS.csv");
        try (var stream = arquivo.getInputStream()) {
            return importService.importar(stream, p.nome(), p.ano(), dryRun, p.username());
        }
    }

    private CensoImportResultDto executarMatriculas(MultipartFile arquivo, Integer anoCenso,
                                                     boolean dryRun, UserDetails user) throws IOException {
        var p = preExec(arquivo, anoCenso, user, "MATRICULA.csv");
        try (var stream = arquivo.getInputStream()) {
            return matriculaImportService.importar(stream, p.nome(), p.ano(), dryRun, p.username());
        }
    }

    private PreExec preExec(MultipartFile arquivo, Integer anoCenso, UserDetails user, String defaultNome) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não fornecido ou vazio.");
        }
        int ano = anoCenso != null ? anoCenso : Year.now().getValue();
        if (ano < 2007 || ano > Year.now().getValue() + 1) {
            throw new IllegalArgumentException("anoCenso fora do intervalo razoável (2007-" + (Year.now().getValue() + 1) + ")");
        }
        String username = user != null ? user.getUsername() : "system";
        String nome = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : defaultNome;
        return new PreExec(ano, username, nome);
    }

    private record PreExec(int ano, String username, String nome) {}
}
