package br.com.caqi.compliance.api.dto;

import br.com.caqi.compliance.domain.entity.LogAuditoria;

import java.time.LocalDateTime;

public record LogAuditoriaDto(
        Long id,
        Long usuarioId,
        String tabela,
        String registroId,
        String acao,
        LocalDateTime carimboTempo,
        String hashAntes,
        String hashDepois
) {
    public static LogAuditoriaDto from(LogAuditoria l) {
        return new LogAuditoriaDto(l.getId(), l.getUsuarioId(), l.getTabela(),
                l.getRegistroId(), l.getAcao(), l.getCarimboTempo(),
                l.getHashAntes(), l.getHashDepois());
    }
}
