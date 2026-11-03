package br.com.fiap.clyvopaws.domain.veterinario;

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

@RestController
@RequestMapping("/veterinarios")
@RequiredArgsConstructor
@Tag(name = "Veterinários", description = "Endpoints para gerenciamento dos veterinários e seus dados. Cadastro e atualização usam multipart/form-data: campo 'dados' (JSON) e campo 'foto' (opcional, JPEG/PNG até 5 MB)")
public class VeterinarioController {

    private final VeterinarioService service;
    private final CadastroComFotoService cadastroService;
    private final LeitorDadosCadastro leitor;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VeterinarioResponseDTO> cadastrar(@RequestParam("dados") String dados,
                                                            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        var response = cadastroService.cadastrarVeterinario(leitor.ler(dados, VeterinarioRequestDTO.class), foto);
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VeterinarioResponseDTO> atualizar(@PathVariable Long id,
                                                            @RequestParam("dados") String dados,
                                                            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.ok(cadastroService.atualizarVeterinario(id, leitor.ler(dados, VeterinarioRequestDTO.class), foto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}