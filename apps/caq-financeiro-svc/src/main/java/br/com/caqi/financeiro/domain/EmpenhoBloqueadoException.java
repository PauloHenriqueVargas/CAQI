package br.com.caqi.financeiro.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lançada quando a flag caqi.compliance.bloquear-empenhos-violadores=true
 * e a despesa proposta derruba alguma vinculação legal. @Transactional
 * faz rollback do INSERT da despesa. Spring devolve 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EmpenhoBloqueadoException extends RuntimeException {
    public EmpenhoBloqueadoException(String motivo) {
        super("Empenho bloqueado: " + motivo);
    }
}
