package br.com.caqi.compliance.api;

import br.com.caqi.compliance.api.dto.NotificacaoDto;
import br.com.caqi.compliance.api.dto.PublicacaoDto;
import br.com.caqi.compliance.domain.repo.NotificacaoRepository;
import br.com.caqi.compliance.domain.repo.PublicacaoPortalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Endpoint público de transparência ativa (LAI) — notificações de violação
 * legal e trilha de publicações LRF art. 48-A são informação pública.
 * Cidadão e controle social (CACS-Fundeb, CME) podem consultar sem auth.
 */
@RestController
@RequestMapping("/api/public/transparencia")
@RequiredArgsConstructor
@Tag(name = "transparencia-publica",
        description = "Histórico público de notificações + trilha de publicações LRF 48-A")
public class PublicTransparenciaController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final NotificacaoRepository repo;
    private final PublicacaoPortalRepository publicacaoRepo;

    @Operation(summary = "Lista todas as notificações (qualquer status) com filtro opcional por ano")
    @GetMapping("/notificacoes")
    public ResponseEntity<List<NotificacaoDto>> notificacoes(@RequestParam(required = false) Integer ano) {
        List<NotificacaoDto> body = (ano != null
                ? repo.findByAnoReferenciaOrderByCreatedAtDesc(ano)
                : repo.findAll())
                .stream().map(NotificacaoDto::from).toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }

    @Operation(summary = "Trilha pública de publicações LRF art. 48-A (transparência ativa)")
    @GetMapping("/publicacoes")
    public ResponseEntity<List<PublicacaoDto>> publicacoes(
            @RequestParam(required = false) String tipo
    ) {
        var lista = (tipo != null)
                ? publicacaoRepo.findByTipoOrderByDataPublicacaoDesc(tipo)
                : publicacaoRepo.findAllByOrderByDataPublicacaoDesc();
        var body = lista.stream().map(PublicacaoDto::fromResumo).toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }

    @Operation(summary = "Detalhe público de uma publicação (inclui snapshot JSON arquivado)")
    @GetMapping("/publicacoes/{id}")
    public ResponseEntity<PublicacaoDto> publicacaoDetalhe(@PathVariable Long id) {
        return publicacaoRepo.findById(id)
                .map(PublicacaoDto::fromDetalhe)
                .map(p -> ResponseEntity.ok().cacheControl(CACHE).body(p))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
