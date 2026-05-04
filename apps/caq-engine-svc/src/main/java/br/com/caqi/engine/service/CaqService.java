package br.com.caqi.engine.service;

import br.com.caqi.engine.api.dto.ItemMemoriaCalculoDto;
import br.com.caqi.engine.api.dto.ItemResultadoDto;
import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaqService {

    private final CaqCalculator calculator;
    private final CalculoCaqRepository calculoRepo;
    private final EtapaRepository etapaRepo;
    private final InsumoRepository insumoRepo;

    /**
     * Executa o cálculo e persiste cada (escola, etapa) — substituindo cálculo prévio do mesmo trio
     * (escola_id, etapa_id, ano), conforme UNIQUE constraint da tabela.
     */
    @Transactional
    public ResultadoCalculoDto calcular(RequisicaoCalculoDto req) {
        log.info("Cálculo CAQ ano={} escolas={} etapas={}",
                req.ano(), req.escolas().size(), req.etapas().size());

        ResultadoCalculoDto resultado = calculator.calcular(req);

        // Cache lookups por código (1 query cada, em vez de N)
        Map<String, Etapa> etapaPorCodigo = new HashMap<>();
        Map<String, Insumo> insumoPorCodigo = new HashMap<>();
        for (String c : req.etapas()) {
            etapaRepo.findByCodigo(c).ifPresent(e -> etapaPorCodigo.put(c, e));
        }
        for (Insumo i : insumoRepo.findAll()) {
            insumoPorCodigo.put(i.getCodigo(), i);
        }

        for (ItemResultadoDto item : resultado.itens()) {
            persistirCalculoItem(req.ano(), item, resultado.memoria(), etapaPorCodigo, insumoPorCodigo);
        }

        return resultado;
    }

    private void persistirCalculoItem(
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
            return;
        }

        // Substitui cálculo anterior (idempotência)
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
            ci.setQtdAplicada(m.qtdAplicada());
            ci.setCustoUnitario(m.custoUnitario());
            ci.setCustoAnual(m.custoAnual());
            ci.setDivisor(m.divisor());
            ci.setCustoAlunoAno(m.custoAlunoAno());
            ci.setBaseCalculo(m.baseCalculo());
            c.addItem(ci);
        }

        calculoRepo.save(c);
    }
}
