package br.com.caqi.engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "insumo")
@Getter
@Setter
@NoArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insumo_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 30)
    private String categoria;

    /** por_aluno | por_turma | por_escola */
    @Column(name = "tipo_aplicacao", nullable = false, length = 15)
    private String tipoAplicacao;

    @Column(nullable = false, length = 20)
    private String unidade;

    /** "Todas" ou lista separada por '/' (ex.: "EF/EM") */
    @Column(name = "etapa_aplicavel", length = 100)
    private String etapaAplicavel;

    @Column(name = "qtd_padrao", nullable = false, precision = 12, scale = 4)
    private BigDecimal qtdPadrao;

    /** Verifica se este insumo se aplica à etapa informada (case-insensitive, prefix match nas partes). */
    public boolean aplicaSeAEtapa(String etapaCodigo) {
        if (etapaAplicavel == null || etapaAplicavel.isBlank() || etapaAplicavel.equalsIgnoreCase("Todas")) {
            return true;
        }
        String alvo = etapaCodigo.toUpperCase();
        for (String parte : etapaAplicavel.split("/")) {
            if (alvo.startsWith(parte.trim().toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
