package br.com.fiap.clyvopaws.domain.consulta;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas", description = "Endpoints para gerenciamento das consultas")
public class ConsultaController {

    private final ConsultaService service;

    @PostMapping
    public ResponseEntity<ConsultaResponseDTO> cadastrar(@RequestBody @Valid ConsultaRequestDTO dto) {
        var response = service.cadastrar(dto);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ConsultaResponseDTO>> listarTodas(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listarTodas(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<Page<ConsultaResponseDTO>> listarHistorico(
            @PathVariable("petId") Long petId,
            @ParameterObject @PageableDefault(size = 5, sort = "dataHora") Pageable pageable) {
        return ResponseEntity.ok(service.listarHistoricoPorPet(petId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsultaResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid ConsultaRequestDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}