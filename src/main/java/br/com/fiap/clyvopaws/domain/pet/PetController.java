package br.com.fiap.clyvopaws.domain.pet;

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
import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Endpoints para gerenciamento dos pets")
public class PetController {
    private final PetService petService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PetResponseDTO> cadastrar(@Valid @RequestBody PetRequestDTO request) {
        PetResponseDTO response = petService.cadastrar(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetResponseDTO> buscarPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(petService.buscarPorId(id));
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<PetResponseDTO>> listarPorTutor(@PathVariable("tutorId") Long tutorId) {
        return ResponseEntity.ok(petService.listarPorTutor(tutorId));
    }

    @GetMapping
    public ResponseEntity<Page<PetResponseDTO>> listarTodos(
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(petService.listarTodosPaginado(pageable));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PetResponseDTO> atualizar(@PathVariable("id") Long id, @Valid @RequestBody PetRequestDTO request) {
        return ResponseEntity.ok(petService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id) {
        petService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}