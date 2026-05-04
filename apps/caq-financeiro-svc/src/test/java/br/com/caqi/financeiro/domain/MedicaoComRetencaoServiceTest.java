package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoComRetencoesDto;
import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoCreateDto;
import br.com.caqi.financeiro.domain.entity.Contrato;
import br.com.caqi.financeiro.domain.entity.Fornecedor;
import br.com.caqi.financeiro.domain.entity.MedicaoContrato;
import br.com.caqi.financeiro.domain.repo.ContratoRepository;
import br.com.caqi.financeiro.domain.repo.FornecedorRepository;
import br.com.caqi.financeiro.domain.repo.MedicaoContratoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedicaoComRetencaoServiceTest {

    private final ContratoRepository contratoRepo = mock(ContratoRepository.class);
    private final FornecedorRepository fornecedorRepo = mock(FornecedorRepository.class);
    private final MedicaoContratoRepository medicaoRepo = mock(MedicaoContratoRepository.class);
    private final MotorRetencoes motor = new MotorRetencoes();
    private final MedicaoComRetencaoService svc =
            new MedicaoComRetencaoService(contratoRepo, fornecedorRepo, medicaoRepo, motor);

    @Test
    @DisplayName("Sem tipoServico/aliquotaIss → persiste medição mas NÃO calcula retenção")
    void semParamsRetencao_naoCalcula() {
        Contrato c = contrato(1L, 10L);
        Fornecedor f = fornecedor(10L, false);
        when(contratoRepo.findById(1L)).thenReturn(Optional.of(c));
        when(fornecedorRepo.findById(10L)).thenReturn(Optional.of(f));
        when(medicaoRepo.save(any())).thenAnswer(inv -> { var m = (MedicaoContrato) inv.getArgument(0); m.setId(99L); return m; });

        var dto = new MedicaoCreateDto("202506", new BigDecimal("5000"), "NF-001", null, null);
        MedicaoComRetencoesDto r = svc.registrarMedicao(1L, dto);

        assertThat(r.medicao().id()).isEqualTo(99L);
        assertThat(r.retencoesPreview()).isNull();
    }

    @Test
    @DisplayName("Com tipoServico+ISS e fornecedor não-optante → calcula retenção completa")
    void naoOptanteComParams_calculaRetencao() {
        Contrato c = contrato(2L, 20L);
        Fornecedor f = fornecedor(20L, false);
        when(contratoRepo.findById(2L)).thenReturn(Optional.of(c));
        when(fornecedorRepo.findById(20L)).thenReturn(Optional.of(f));
        when(medicaoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new MedicaoCreateDto("202507", new BigDecimal("10000"), "NF-002",
                "GERAL", new BigDecimal("5.0"));
        MedicaoComRetencoesDto r = svc.registrarMedicao(2L, dto);

        assertThat(r.retencoesPreview()).isNotNull();
        // IRRF 1,5% + PIS 0,65% + COFINS 3% + CSLL 1% + ISS 5% = 11,15% × 10.000 = 1.115
        assertThat(r.retencoesPreview().totalRetido()).isEqualByComparingTo("1115.00");
        assertThat(r.retencoesPreview().valorLiquido()).isEqualByComparingTo("8885.00");
    }

    @Test
    @DisplayName("Optante Simples → preview retorna apenas ISS")
    void optanteSimples_apenasIss() {
        Contrato c = contrato(3L, 30L);
        Fornecedor f = fornecedor(30L, true);
        when(contratoRepo.findById(3L)).thenReturn(Optional.of(c));
        when(fornecedorRepo.findById(30L)).thenReturn(Optional.of(f));
        when(medicaoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new MedicaoCreateDto("202508", new BigDecimal("10000"), "NF-003",
                "GERAL", new BigDecimal("5.0"));
        MedicaoComRetencoesDto r = svc.registrarMedicao(3L, dto);

        assertThat(r.retencoesPreview().iss()).isEqualByComparingTo("500.00");
        assertThat(r.retencoesPreview().irrf()).isEqualByComparingTo("0");
        assertThat(r.retencoesPreview().totalRetido()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Contrato inexistente → 404")
    void contratoInexistente_404() {
        when(contratoRepo.findById(99L)).thenReturn(Optional.empty());
        var dto = new MedicaoCreateDto("202509", new BigDecimal("100"), null, null, null);
        assertThatThrownBy(() -> svc.registrarMedicao(99L, dto))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static Contrato contrato(Long id, Long fornecedorId) {
        Contrato c = new Contrato();
        c.setId(id);
        c.setFornecedorId(fornecedorId);
        return c;
    }

    private static Fornecedor fornecedor(Long id, boolean optante) {
        Fornecedor f = new Fornecedor();
        f.setId(id);
        f.setOptanteSimples(optante);
        f.setCnpj("00.000.000/0001-00");
        return f;
    }
}
