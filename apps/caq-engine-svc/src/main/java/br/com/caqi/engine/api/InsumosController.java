package br.com.caqi.engine.api;

import br.com.caqi.engine.api.dto.CustoInsumoDtos.CustoInsumoCreateDto;
import br.com.caqi.engine.api.dto.CustoInsumoDtos.CustoInsumoDto;
import br.com.caqi.engine.api.dto.InsumoDtos.InsumoDto;
import br.com.caqi.engine.api.dto.InsumoDtos.InsumoUpsertDto;
import br.com.caqi.engine.core.exception.NotFoundException;
import br.com.caqi.engine.domain.entity.Insumo;
import br.com.caqi.engine.domain.repo.CustoInsumoRepository;
import br.com.caqi.engine.domain.repo.InsumoRepository;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/caqi/insumos")
@RequiredArgsConstructor
@Tag(name = "caqi-insumos", description = "Catálogo de insumos do CAQ/CAQi e seus custos vigentes")
public class InsumosController {

    private final InsumoRepository insumoRepo;
    private final CustoInsumoRepository custoRepo;

    @Operation(summary = "Lista todos os insumos do catálogo")
    @GetMapping
    public List<InsumoDto> listar() {
        return insumoRepo.findAll().stream().map(InsumoDto::from).toList();
    }

    @Operation(summary = "Busca insumo por código")
    @GetMapping("/{codigo}")
    public InsumoDto buscar(@PathVariable String codigo) {
        return InsumoDto.from(getOrThrow(codigo));
    }

    @Operation(summary = "Cria novo insumo")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InsumoDto criar(@Valid @RequestBody InsumoUpsertDto dto) {
        if (insumoRepo.findByCodigo(dto.codigo()).isPresent()) {
            throw new IllegalArgumentException("Já existe insumo com código " + dto.codigo());
        }
        return InsumoDto.from(insumoRepo.save(dto.toEntity(null)));
    }

    @Operation(summary = "Atualiza insumo existente (identificado por código). Não muda o id.")
    @PutMapping("/{codigo}")
    @Transactional
    public InsumoDto atualizar(@PathVariable String codigo, @Valid @RequestBody InsumoUpsertDto dto) {
        Insumo existente = getOrThrow(codigo);
        if (!existente.getCodigo().equals(dto.codigo())) {
            throw new IllegalArgumentException("Não é permitido alterar o código do insumo");
        }
        return InsumoDto.from(insumoRepo.save(dto.toEntity(existente)));
    }

    @Operation(summary = "Lista todas as vigências de custo de um insumo (CAQi + CAQ adequado)")
    @GetMapping("/{codigo}/custos")
    public List<CustoInsumoDto> listarCustos(@PathVariable String codigo) {
        Insumo i = getOrThrow(codigo);
        return custoRepo.findByInsumoIdOrderByVigenciaInicioDesc(i.getId()).stream()
                .map(CustoInsumoDto::from)
                .toList();
    }

    @Operation(summary = "Cria nova vigência de custo (não substitui a vigência anterior — adiciona nova)")
    @PostMapping("/{codigo}/custos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CustoInsumoDto criarCusto(@PathVariable String codigo, @Valid @RequestBody CustoInsumoCreateDto dto) {
        Insumo i = getOrThrow(codigo);
        return CustoInsumoDto.from(custoRepo.save(dto.toEntity(i.getId())));
    }

    private Insumo getOrThrow(String codigo) {
        return insumoRepo.findByCodigo(codigo)
                .orElseThrow(() -> new NotFoundException("Insumo", codigo));
    }
}
