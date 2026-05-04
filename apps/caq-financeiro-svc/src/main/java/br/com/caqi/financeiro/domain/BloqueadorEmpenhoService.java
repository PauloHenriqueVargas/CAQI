package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.domain.entity.Despesa;
import br.com.caqi.financeiro.domain.repo.DespesaRepository;
import br.com.caqi.shared.dto.ExecucaoFundebDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persiste a despesa e VERIFICA se ela derruba alguma vinculação legal.
 * Quando a flag caqi.compliance.bloquear-empenhos-violadores=true (default
 * false em prod), simula impacto:
 *   antes  = execução do ano sem a despesa
 *   save + flush
 *   depois = execução do ano com a despesa
 * Se transição cumpre TRUE→FALSE detectada, lança EmpenhoBloqueadoException
 * — @Transactional rollback desfaz o INSERT.
 *
 * Este é o "interceptor" REST mencionado no spec original (bloqueio de
 * empenho que viola regra legal) sem usar HandlerInterceptor — é uma
 * checagem semântica no domínio, mais robusta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BloqueadorEmpenhoService {

    private final DespesaRepository despesaRepo;
    private final FundebService fundebService;
    private final AnalisadorImpactoFundeb analisador;
    private final EntityManager entityManager;

    @Value("${caqi.compliance.bloquear-empenhos-violadores:false}")
    private boolean bloquear;

    @Transactional
    public Despesa criarComCheck(Despesa nova) {
        if (!bloquear || nova.getCompetencia() == null) {
            return despesaRepo.save(nova);
        }

        int ano;
        try {
            ano = Integer.parseInt(nova.getCompetencia().substring(0, 4));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.warn("Competencia inválida {} — pulando bloqueador", nova.getCompetencia());
            return despesaRepo.save(nova);
        }

        ExecucaoFundebDto antes = fundebService.calcular(ano);

        Despesa salva = despesaRepo.save(nova);
        entityManager.flush();   // garante que aparece nas próximas queries dentro da mesma tx

        ExecucaoFundebDto depois = fundebService.calcular(ano);
        Optional<String> motivo = analisador.detectarTransicaoNegativa(antes, depois);
        if (motivo.isPresent()) {
            log.warn("Empenho ID temporário={} BLOQUEADO: {}", salva.getId(), motivo.get());
            throw new EmpenhoBloqueadoException(motivo.get());
        }

        return salva;
    }
}
