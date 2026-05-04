package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.FonteRecursoDtos.FonteRecursoCreateDto;
import br.com.caqi.financeiro.api.dto.FonteRecursoDtos.FonteRecursoDto;
import br.com.caqi.financeiro.domain.repo.FonteRecursoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/financeiro/fontes-recurso")
@RequiredArgsConstructor
@Tag(name = "financeiro-fontes-recurso", description = "Catálogo de fontes de recurso (Propria, VAAF, VAAT, VAAR, Outras)")
public class FontesRecursoController {

    private final FonteRecursoRepository repo;

    @GetMapping
    public List<FonteRecursoDto> listar() {
        return repo.findAll().stream().map(FonteRecursoDto::from).toList();
    }

    @Operation(summary = "Cadastra nova fonte de recurso (operação ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public FonteRecursoDto criar(@Valid @RequestBody FonteRecursoCreateDto dto) {
        if (repo.findByTipo(dto.tipo()).isPresent()) {
            throw new IllegalArgumentException("Já existe fonte tipo=" + dto.tipo());
        }
        return FonteRecursoDto.from(repo.save(dto.toEntity()));
    }
}
