package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.FornecedorDtos.FornecedorDto;
import br.com.caqi.financeiro.api.dto.FornecedorDtos.FornecedorUpsertDto;
import br.com.caqi.financeiro.domain.entity.Fornecedor;
import br.com.caqi.financeiro.domain.repo.FornecedorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/financeiro/fornecedores")
@RequiredArgsConstructor
@Tag(name = "financeiro-fornecedores", description = "Cadastro de fornecedores (PJ; flag optante Simples define regime de retenções)")
public class FornecedoresController {

    private final FornecedorRepository repo;

    @GetMapping
    public List<FornecedorDto> listar() {
        return repo.findAll().stream().map(FornecedorDto::from).toList();
    }

    @GetMapping("/{id}")
    public FornecedorDto buscar(@PathVariable Long id) {
        return FornecedorDto.from(getOrThrow(id));
    }

    @Operation(summary = "Cria fornecedor (CNPJ deve ser único)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public FornecedorDto criar(@Valid @RequestBody FornecedorUpsertDto dto) {
        if (repo.findByCnpj(dto.cnpj()).isPresent()) {
            throw new IllegalArgumentException("CNPJ já cadastrado: " + dto.cnpj());
        }
        return FornecedorDto.from(repo.save(dto.toEntity(null)));
    }

    @PutMapping("/{id}")
    @Transactional
    public FornecedorDto atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorUpsertDto dto) {
        Fornecedor existente = getOrThrow(id);
        return FornecedorDto.from(repo.save(dto.toEntity(existente)));
    }

    private Fornecedor getOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor " + id));
    }
}
