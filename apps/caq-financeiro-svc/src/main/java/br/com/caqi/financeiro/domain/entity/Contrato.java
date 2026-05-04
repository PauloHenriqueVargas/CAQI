package br.com.caqi.financeiro.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contrato")
@Getter
@Setter
@NoArgsConstructor
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contrato_id")
    private Long id;

    @Column(name = "fornecedor_id", nullable = false)
    private Long fornecedorId;

    @Column(nullable = false)
    private String objeto;

    @Column(name = "data_assinatura", nullable = false)
    private LocalDate dataAssinatura;

    @Column(name = "valor_global", precision = 14, scale = 2)
    private BigDecimal valorGlobal;

    /** ex.: PREGAO_ELETRONICO, CONCORRENCIA, DISPENSA, INEXIGIBILIDADE, DIALOGO_COMPETITIVO */
    @Column(length = 30)
    private String modalidade;

    /** Identificador no Portal Nacional de Contratações Públicas (PNCP). Preenchido após publicação. */
    @Column(name = "pncp_id", length = 60)
    private String pncpId;
}
