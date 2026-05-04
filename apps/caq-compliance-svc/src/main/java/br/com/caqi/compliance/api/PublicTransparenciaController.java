package br.com.caqi.compliance.api;

import br.com.caqi.compliance.api.dto.NotificacaoDto;
import br.com.caqi.compliance.domain.repo.NotificacaoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Endpoint público de transparência ativa (LAI) — notificações de violação
 * legal e seu tratamento são informação pública. Cidadão e controle social
 * (CACS-Fundeb, CME) podem consultar sem auth.
 */
@RestController
@RequestMapping("/api/public/transparencia")
@RequiredArgsConstructor
@Tag(name = "transparencia-publica",
        description = "Histórico público de notificações de compliance — controle social pode auditar")
public class PublicTransparenciaController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final NotificacaoRepository repo;

    @Operation(summary = "Lista todas as notificações (qualquer status) com filtro opcional por ano")
    @GetMapping("/notificacoes")
    public ResponseEntity<List<NotificacaoDto>> notificacoes(@RequestParam(required = false) Integer ano) {
        List<NotificacaoDto> body = (ano != null
                ? repo.findByAnoReferenciaOrderByCreatedAtDesc(ano)
                : repo.findAll())
                .stream().map(NotificacaoDto::from).toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(body);
    }
}
