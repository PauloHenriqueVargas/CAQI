package br.com.caqi.compliance.api;

import br.com.caqi.compliance.api.dto.PublicacaoDto;
import br.com.caqi.compliance.domain.PublicacaoService;
import br.com.caqi.compliance.domain.PublicacaoService.ResultadoPublicacao;
import br.com.caqi.compliance.domain.repo.PublicacaoPortalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance/publicacoes")
@RequiredArgsConstructor
@Tag(name = "publicacoes",
        description = "Trilha de publicações automáticas LRF art. 48-A (transparência ativa)")
public class PublicacaoController {

    private final PublicacaoService service;
    private final PublicacaoPortalRepository repo;

    @Operation(summary = "Executa o ciclo de publicação imediatamente (manual). Idempotente por hash.")
    @PostMapping("/executar")
    public ResultadoPublicacao executar(
            @RequestParam(required = false) Integer ano,
            @AuthenticationPrincipal UserDetails user
    ) {
        String username = user != null ? user.getUsername() : "system";
        return service.executarTudo(ano, "manual:" + username);
    }

    @Operation(summary = "Lista publicações (mais recentes primeiro). Filtros opcionais por tipo e referência.")
    @GetMapping
    public List<PublicacaoDto> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String referencia
    ) {
        var lista = (tipo != null)
                ? repo.findByTipoOrderByDataPublicacaoDesc(tipo)
                : (referencia != null)
                    ? repo.findByReferenciaOrderByDataPublicacaoDesc(referencia)
                    : repo.findAllByOrderByDataPublicacaoDesc();
        return lista.stream().map(PublicacaoDto::fromResumo).toList();
    }

    @Operation(summary = "Detalhe de uma publicação incluindo o snapshot JSON do conteúdo")
    @GetMapping("/{id}")
    public ResponseEntity<PublicacaoDto> detalhe(@PathVariable Long id) {
        return repo.findById(id)
                .map(PublicacaoDto::fromDetalhe)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
