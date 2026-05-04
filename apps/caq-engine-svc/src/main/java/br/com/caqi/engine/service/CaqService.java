package br.com.caqi.engine.service;

import br.com.caqi.engine.api.dto.RequisicaoCalculoDto;
import br.com.caqi.engine.api.dto.ResultadoCalculoDto;
import br.com.caqi.engine.domain.CaqCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaqService {

    private final CaqCalculator calculator;

    @Transactional(readOnly = true)
    public ResultadoCalculoDto calcular(RequisicaoCalculoDto requisicao) {
        log.info("Cálculo CAQ ano={} escolas={} etapas={}",
                requisicao.ano(), requisicao.escolas().size(), requisicao.etapas().size());
        return calculator.calcular(requisicao);
    }
}
