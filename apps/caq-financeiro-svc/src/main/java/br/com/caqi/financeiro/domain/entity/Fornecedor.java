package br.com.caqi.financeiro.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fornecedor")
@Getter
@Setter
@NoArgsConstructor
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fornecedor_id")
    private Long id;

    @Column(unique = true, length = 18)
    private String cnpj;

    private String nome;

    /** Determina o regime de retenções (LC 123/2006). */
    @Column(name = "optante_simples", nullable = false)
    private Boolean optanteSimples = false;

    @Column(length = 60)
    private String municipio;
}
