package br.com.caqi.escolar.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "etapa")
@Getter
@Setter
@NoArgsConstructor
public class Etapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "etapa_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(length = 30)
    private String modalidade;

    private String descricao;
}
