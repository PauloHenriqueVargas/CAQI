package br.com.caqi.engine.domain;

/** Perfis de qualidade para cálculo do CAQ/CAQi. Valores correspondem ao CHECK do banco. */
public final class Perfil {
    public static final String MINIMO = "minimo";       // CAQi — piso mínimo (PNE Meta 20)
    public static final String ADEQUADO = "adequado";   // CAQ  — padrão adequado

    private Perfil() {}
}
