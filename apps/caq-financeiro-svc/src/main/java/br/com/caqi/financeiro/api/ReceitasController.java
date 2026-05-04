package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.ReceitaDtos.ReceitaCreateDto;
import br.com.caqi.financeiro.api.dto.ReceitaDtos.ReceitaDto;
import br.com.caqi.financeiro.domain.repo.ReceitaRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/financeiro/receitas")
@RequiredArgsConstructor
@Tag(name = "financeiro-receitas", description = "Lançamento de receitas (impostos, transferências, Fundeb)")
public class ReceitasController {

    private final ReceitaRepository receitaRepo;

    @Operation(summary = "Lista receitas. Filtre por ano (4 dígitos) opcional.")
    @GetMapping
    public List<ReceitaDto> listar(@RequestParam(required = false) Integer ano) {
        if (ano == null) {
            return receitaRepo.findAll().stream().map(ReceitaDto::from).toList();
        }
        String inicio = String.format("%04d01", ano);
        String fim    = String.format("%04d12", ano);
        return receitaRepo.listarDoAno(inicio, fim).stream().map(ReceitaDto::from).toList();
    }

    @Operation(summary = "Cria lançamento de receita")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ReceitaDto criar(@Valid @RequestBody ReceitaCreateDto dto) {
        return ReceitaDto.from(receitaRepo.save(dto.toEntity()));
    }
}
