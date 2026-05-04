package br.com.caqi.financeiro.api;

import br.com.caqi.financeiro.api.dto.DespesaDtos.DespesaCreateDto;
import br.com.caqi.financeiro.api.dto.DespesaDtos.DespesaDto;
import br.com.caqi.financeiro.domain.BloqueadorEmpenhoService;
import br.com.caqi.financeiro.domain.repo.DespesaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/financeiro/despesas")
@RequiredArgsConstructor
@Tag(name = "financeiro-despesas", description = "Lançamento de despesas (pessoal, capital, custeio)")
public class DespesasController {

    private final DespesaRepository despesaRepo;
    private final BloqueadorEmpenhoService bloqueador;

    @Operation(summary = "Lista despesas. Filtre por ano (4 dígitos) opcional.")
    @GetMapping
    public List<DespesaDto> listar(@RequestParam(required = false) Integer ano) {
        if (ano == null) {
            return despesaRepo.findAll().stream().map(DespesaDto::from).toList();
        }
        String inicio = String.format("%04d01", ano);
        String fim    = String.format("%04d12", ano);
        return despesaRepo.listarDoAno(inicio, fim).stream().map(DespesaDto::from).toList();
    }

    @Operation(summary = "Cria lançamento de despesa. Quando caqi.compliance.bloquear-empenhos-violadores=true, " +
            "rejeita (409 Conflict) se a despesa derruba alguma vinculação cumpre→não-cumpre.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DespesaDto criar(@Valid @RequestBody DespesaCreateDto dto) {
        return DespesaDto.from(bloqueador.criarComCheck(dto.toEntity()));
    }
}
