package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "indice_preco",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nome", "competencia"}))
@Getter
@Setter
@NoArgsConstructor
public class IndicePreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indice_id")
    private Long id;

    /** ex.: "IPCA", "SINAPI_Obras" */
    @Column(nullable = false, length = 50)
    private String nome;

    /** Formato YYYY-MM (CHAR(7) na DDL) */
    @Column(nullable = false, length = 7)
    private String competencia;

    /** Variação percentual mensal (ex.: 0.45 = +0,45% no mês) */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal valor;
}
