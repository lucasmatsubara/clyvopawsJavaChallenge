package br.com.fiap.clyvopaws.domain.pet;

import br.com.fiap.clyvopaws.domain.foto.CadastroComFotoService;
import br.com.fiap.clyvopaws.domain.foto.LeitorDadosCadastro;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Endpoints para gerenciamento dos pets. Cadastro e atualização usam multipart/form-data: campo 'dados' (JSON) e campo 'foto' (opcional, JPEG/PNG até 5 MB)")
public class PetController {
    private final PetService petService;
    private final CadastroComFotoService cadastroService;
    private final LeitorDadosCadastro leitor;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponseDTO> cadastrar(@RequestParam("dados") String dados,
                                                    @RequestPart(value = "foto", required = false) MultipartFile foto) {
        PetResponseDTO response = cadastroService.cadastrarPet(leitor.ler(dados, PetRequestDTO.class), foto);
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponseDTO> atualizar(@PathVariable("id") Long id,
                                                    @RequestParam("dados") String dados,
                                                    @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(cadastroService.atualizarPet(id, leitor.ler(dados, PetRequestDTO.class), foto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id) {
        petService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}