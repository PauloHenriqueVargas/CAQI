package br.com.caqi.engine.domain.repo;

import br.com.caqi.engine.domain.entity.UsuarioMfa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioMfaRepository extends JpaRepository<UsuarioMfa, String> {
}
