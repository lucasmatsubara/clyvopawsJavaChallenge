package br.com.fiap.clyvopaws.domain.tutor;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/tutores")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Endpoints para gerenciamento de tutores e seus dados")
public class TutorController {
    private final TutorService tutorService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TutorResponseDTO> cadastrar(@Valid @RequestBody TutorRequestDTO request) {
        TutorResponseDTO response = tutorService.cadastrar(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TutorResponseDTO> buscarPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(tutorService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<TutorResponseDTO>> listarTodos(
            @ParameterObject @PageableDefault(size = 10, sort = "nomeCompleto") Pageable pageable) {
        return ResponseEntity.ok(tutorService.listarTodos(pageable));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TutorResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody TutorUpdateDTO dto) {
        return ResponseEntity.ok(tutorService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id) {
        tutorService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}