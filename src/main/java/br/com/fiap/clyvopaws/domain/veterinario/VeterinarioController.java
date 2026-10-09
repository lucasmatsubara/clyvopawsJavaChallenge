package br.com.fiap.clyvopaws.domain.veterinario;

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
@RequestMapping("/veterinarios")
@RequiredArgsConstructor
@Tag(name = "Veterinários", description = "Endpoints para gerenciamento dos veterinários e seus dados")
public class VeterinarioController {

    private final VeterinarioService service;

    @PostMapping
    public ResponseEntity<VeterinarioResponseDTO> cadastrar(@RequestBody @Valid VeterinarioRequestDTO dto) {
        var response = service.cadastrar(dto);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<VeterinarioResponseDTO>> listarTodos(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listarTodos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeterinarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarDtoPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeterinarioResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid VeterinarioRequestDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}