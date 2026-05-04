package br.com.caqi.financeiro.domain;

import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoComRetencoesDto;
import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoCreateDto;
import br.com.caqi.financeiro.api.dto.MedicaoDtos.MedicaoDto;
import br.com.caqi.financeiro.api.dto.RetencaoDtos.RequisicaoRetencaoDto;
import br.com.caqi.financeiro.api.dto.RetencaoDtos.ResultadoRetencaoDto;
import br.com.caqi.financeiro.domain.entity.Contrato;
import br.com.caqi.financeiro.domain.entity.Fornecedor;
import br.com.caqi.financeiro.domain.entity.MedicaoContrato;
import br.com.caqi.financeiro.domain.repo.ContratoRepository;
import br.com.caqi.financeiro.domain.repo.FornecedorRepository;
import br.com.caqi.financeiro.domain.repo.MedicaoContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Persiste a medição contratual e, opcionalmente, devolve preview das retenções
 * tributárias para emissão da guia de pagamento.
 *
 * Quando tipoServico/aliquotaIssMunicipal informados, chama MotorRetencoes.
 * O optanteSimples vem do Fornecedor (não da requisição) — fonte única da verdade.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicaoComRetencaoService {

    private final ContratoRepository contratoRepo;
    private final FornecedorRepository fornecedorRepo;
    private final MedicaoContratoRepository medicaoRepo;
    private final MotorRetencoes motorRetencoes;

    @Transactional
    public MedicaoComRetencoesDto registrarMedicao(Long contratoId, MedicaoCreateDto dto) {
        Contrato contrato = contratoRepo.findById(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contrato não encontrado: " + contratoId));

        Fornecedor fornecedor = fornecedorRepo.findById(contrato.getFornecedorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Contrato " + contratoId + " referencia fornecedor inexistente: " + contrato.getFornecedorId()));

        MedicaoContrato salva = medicaoRepo.save(dto.toEntity(contratoId));

        ResultadoRetencaoDto preview = null;
        if (dto.tipoServico() != null && dto.aliquotaIssMunicipal() != null) {
            var req = new RequisicaoRetencaoDto(
                    dto.valorMedido(),
                    fornecedor.getCnpj(),
                    fornecedor.getOptanteSimples(),
                    null,                       // naturezaDespesa — opcional aqui
                    dto.tipoServico(),
                    dto.aliquotaIssMunicipal(),
                    false
            );
            preview = motorRetencoes.calcular(req);
            log.info("Medição id={} contrato={} valor={} → retido R$ {} líquido R$ {}",
                    salva.getId(), contratoId, dto.valorMedido(),
                    preview.totalRetido(), preview.valorLiquido());
        }

        return new MedicaoComRetencoesDto(MedicaoDto.from(salva), preview);
    }
}
