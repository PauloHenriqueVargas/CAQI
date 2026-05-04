package br.com.caqi.compliance.api;

import br.com.caqi.compliance.api.dto.NotificacaoDto;
import br.com.caqi.compliance.domain.entity.Notificacao;
import br.com.caqi.compliance.domain.repo.NotificacaoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/compliance/notificacoes")
@RequiredArgsConstructor
@Tag(name = "compliance-notificacoes", description = "Histórico de notificações geradas pelo avaliador")
public class NotificacoesController {

    private final NotificacaoRepository repo;

    @Operation(summary = "Lista notificações. Filtre por status (default: aberta) ou ano.")
    @GetMapping
    public List<NotificacaoDto> listar(
            @RequestParam(required = false, defaultValue = "aberta") String status,
            @RequestParam(required = false) Integer ano) {
        List<Notificacao> result = (ano != null)
                ? repo.findByAnoReferenciaOrderByCreatedAtDesc(ano)
                : repo.findByStatusOrderByCreatedAtDesc(status);
        return result.stream().map(NotificacaoDto::from).toList();
    }

    @Operation(summary = "Atualiza status de uma notificação (em_analise | resolvida | ignorada)")
    @PatchMapping("/{id}/status")
    @Transactional
    public NotificacaoDto atualizarStatus(@PathVariable Long id, @RequestParam String novo) {
        if (!List.of("em_analise", "resolvida", "ignorada").contains(novo)) {
            throw new IllegalArgumentException("status inválido: " + novo);
        }
        Notificacao n = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        n.setStatus(novo);
        if ("resolvida".equals(novo) || "ignorada".equals(novo)) {
            n.setResolvedAt(Instant.now());
        }
        return NotificacaoDto.from(repo.save(n));
    }
}
