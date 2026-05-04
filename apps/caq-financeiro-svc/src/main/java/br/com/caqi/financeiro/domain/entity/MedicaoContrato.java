package br.com.caqi.financeiro.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "medicao_contrato")
@Getter
@Setter
@NoArgsConstructor
public class MedicaoContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medicao_id")
    private Long id;

    @Column(name = "contrato_id", nullable = false)
    private Long contratoId;

    /** YYYYMM */
    @Column(nullable = false, length = 6)
    private String competencia;

    @Column(name = "valor_medido", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorMedido;

    @Column(name = "nota_fiscal", length = 40)
    private String notaFiscal;
}
