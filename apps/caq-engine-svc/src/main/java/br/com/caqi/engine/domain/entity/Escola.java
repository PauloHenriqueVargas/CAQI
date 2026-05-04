package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "escola")
@Getter
@Setter
@NoArgsConstructor
public class Escola {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escola_id")
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "inep_id", length = 20)
    private String inepId;

    @Column(length = 20)
    private String rede;

    @Column(length = 10)
    private String localizacao;

    @Column(length = 10)
    private String situacao;
}
