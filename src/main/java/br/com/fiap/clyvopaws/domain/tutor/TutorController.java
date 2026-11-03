package br.com.fiap.clyvopaws.domain.tutor;

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

@RestController
@RequestMapping("/tutores")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Endpoints para gerenciamento de tutores e seus dados. Cadastro e atualização usam multipart/form-data: campo 'dados' (JSON) e campo 'foto' (opcional, JPEG/PNG até 5 MB)")
public class TutorController {
    private final TutorService tutorService;
    private final CadastroComFotoService cadastroService;
    private final LeitorDadosCadastro leitor;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorResponseDTO> cadastrar(@RequestParam("dados") String dados,
                                                      @RequestPart(value = "foto", required = false) MultipartFile foto) {
        TutorResponseDTO response = cadastroService.cadastrarTutor(leitor.ler(dados, TutorRequestDTO.class), foto);
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorResponseDTO> atualizar(@PathVariable("id") Long id,
                                                      @RequestParam("dados") String dados,
                                                      @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(cadastroService.atualizarTutor(id, leitor.ler(dados, TutorUpdateDTO.class), foto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id) {
        tutorService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}