package br.com.caqi.engine.core.exception;

/** Recurso solicitado não existe — handler global devolve 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String recurso, Object id) {
        super(recurso + " não encontrado: " + id);
    }

    public NotFoundException(String mensagem) {
        super(mensagem);
    }
}
