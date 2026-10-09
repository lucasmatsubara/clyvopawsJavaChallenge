package br.com.fiap.clyvopaws.domain.clinica;

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
@RequestMapping("/clinicas")
@RequiredArgsConstructor
@Tag(name = "Clínicas", description = "Endpoints para gerenciamento das clínicas")
public class ClinicaController {

    private final ClinicaService service;

    @PostMapping
    public ResponseEntity<ClinicaResponseDTO> cadastrar(@RequestBody @Valid ClinicaRequestDTO dto) {
        var response = service.cadastrar(dto);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ClinicaResponseDTO>> listarTodas(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listarTodas(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarDtoPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicaResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid ClinicaRequestDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}