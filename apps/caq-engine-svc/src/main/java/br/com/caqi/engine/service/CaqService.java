package br.com.caqi.engine.service;

import br.com.caqi.engine.api.dto.CalculoExecutadoEvent;
import br.com.caqi.engine.api.dto.ItemMemoriaCalculoDto;
import br.com.caqi.engine.api.dto.ItemResultadoDto;
import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
import br.com.caqi.engine.core.messaging.CaqEventPublisher;
import br.com.caqi.engine.domain.CaqCalculator;
import br.com.caqi.engine.domain.entity.CalculoCaq;
import br.com.caqi.engine.domain.entity.CalculoCaqItem;
import br.com.caqi.engine.domain.entity.Etapa;
import br.com.caqi.engine.domain.entity.Insumo;
import br.com.caqi.engine.domain.repo.CalculoCaqRepository;
import br.com.caqi.engine.domain.repo.EtapaRepository;
import br.com.caqi.engine.domain.repo.InsumoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaqService {

    private final CaqCalculator calculator;
    private final CalculoCaqRepository calculoRepo;
    private final EtapaRepository etapaRepo;
    private final InsumoRepository insumoRepo;
    /** Optional — pode ser ausente se caqi.events.enabled=false (ex.: perfil test). */
    private final Optional<CaqEventPublisher> eventPublisher;

    @Value("${caqi.tenant.municipio-id:000000}")
    private String municipioId;

    /**
     * Executa o cálculo, persiste cada (escola, etapa) — substituindo cálculo prévio do mesmo
     * trio (escola_id, etapa_id, ano) — e publica CalculoExecutadoEvent (fire-and-forget) para
     * cada cálculo persistido.
     */
    @Transactional
    public ResultadoCalculoDto calcular(RequisicaoCalculoDto req) {
        log.info("Cálculo CAQ ano={} escolas={} etapas={}",
                req.ano(), req.escolas().size(), req.etapas().size());

        ResultadoCalculoDto resultado = calculator.calcular(req);

        Map<String, Etapa> etapaPorCodigo = new HashMap<>();
        Map<String, Insumo> insumoPorCodigo = new HashMap<>();
        for (String c : req.etapas()) {
            etapaRepo.findByCodigo(c).ifPresent(e -> etapaPorCodigo.put(c, e));
        }
        for (Insumo i : insumoRepo.findAll()) {
            insumoPorCodigo.put(i.getCodigo(), i);
        }

        List<CalculoExecutadoEvent> eventos = new ArrayList<>();
        for (ItemResultadoDto item : resultado.itens()) {
            CalculoCaq persistido = persistirCalculoItem(
                    req.ano(), item, resultado.memoria(), etapaPorCodigo, insumoPorCodigo);
            if (persistido != null) {
                eventos.add(CalculoExecutadoEvent.novo(
                        municipioId, persistido.getId(), persistido.getEscolaId(),
                        item.etapaId(), persistido.getAno(),
                        item.caqiAlunoAno(), item.caqAlunoAno(), item.gapAlunoAno()));
            }
        }

        // TODO Fase 9: trocar por TransactionalEventListener AFTER_COMMIT — hoje publicamos antes do commit.
        eventPublisher.ifPresent(p -> eventos.forEach(p::publicar));

        return resultado;
    }

    private CalculoCaq persistirCalculoItem(
            int ano,
            ItemResultadoDto item,
            List<ItemMemoriaCalculoDto> memoriaCompleta,
            Map<String, Etapa> etapaPorCodigo,
            Map<String, Insumo> insumoPorCodigo
    ) {
        Long escolaId = Long.valueOf(item.escolaId());
        Etapa etapa = etapaPorCodigo.get(item.etapaId());
        if (etapa == null) {
            log.warn("Persistência ignorada — etapa {} não encontrada", item.etapaId());
            return null;
        }

        calculoRepo.findByEscolaIdAndEtapaIdAndAno(escolaId, etapa.getId(), ano)
                .ifPresent(calculoRepo::delete);

        CalculoCaq c = new CalculoCaq();
        c.setEscolaId(escolaId);
        c.setEtapaId(etapa.getId());
        c.setAno(ano);
        c.setValorCaqiAlunoAno(item.caqiAlunoAno());
        c.setValorCaqAlunoAno(item.caqAlunoAno());
        c.setGapExecucao(item.gapAlunoAno());

        for (ItemMemoriaCalculoDto m : memoriaCompleta) {
            if (!m.escolaId().equals(item.escolaId()) || !m.etapaCodigo().equals(item.etapaId())) continue;
            Insumo insumo = insumoPorCodigo.get(m.insumoCodigo());
            if (insumo == null) continue;

            CalculoCaqItem ci = new CalculoCaqItem();
            ci.setInsumoId(insumo.getId());
            ci.setInsumoCodigo(m.insumoCodigo());
            ci.setInsumoNome(m.insumoNome());
            ci.setTipoAplicacao(m.tipoAplicacao());
            ci.setPerfil(m.perfil());
            ci.setQtdAplicada(m.qtdAplicada());
            ci.setCustoUnitario(m.custoUnitario());
            ci.setCustoAnual(m.custoAnual());
            ci.setDivisor(m.divisor());
            ci.setCustoAlunoAno(m.custoAlunoAno());
            ci.setBaseCalculo(m.baseCalculo());
            c.addItem(ci);
        }

        return calculoRepo.save(c);
    }
}
