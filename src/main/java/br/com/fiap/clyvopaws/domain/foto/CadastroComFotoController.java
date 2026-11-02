package br.com.fiap.clyvopaws.domain.foto;

import br.com.fiap.clyvopaws.domain.pet.PetRequestDTO;
import br.com.fiap.clyvopaws.domain.pet.PetResponseDTO;
import br.com.fiap.clyvopaws.domain.tutor.TutorRequestDTO;
import br.com.fiap.clyvopaws.domain.tutor.TutorResponseDTO;
import br.com.fiap.clyvopaws.domain.tutor.TutorUpdateDTO;
import br.com.fiap.clyvopaws.domain.veterinario.VeterinarioRequestDTO;
import br.com.fiap.clyvopaws.domain.veterinario.VeterinarioResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.stream.Collectors;

/** As mesmas URLs aceitam JSON (controllers antigos) ou multipart com dados + foto. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Cadastros com foto", description = "Cadastro e atualização de tutores, pets e veterinários com foto (multipart/form-data: campo 'dados' com o JSON e campo 'foto' com a imagem JPEG/PNG de até 5 MB)")
public class CadastroComFotoController {
    private final CadastroComFotoService service;
    private final ObjectMapper mapper;
    private final Validator validator;

    @PostMapping(value = "/tutores", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorResponseDTO> cadastrarTutor(
            @RequestParam("dados") String dados, @RequestPart("foto") MultipartFile foto) {
        var resposta = service.cadastrarTutor(lerDados(dados, TutorRequestDTO.class), foto);
        return ResponseEntity.created(URI.create("/tutores/" + resposta.id())).body(resposta);
    }

    @PutMapping(value = "/tutores/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TutorResponseDTO atualizarTutor(@PathVariable("id") Long id,
                                           @RequestParam("dados") String dados, @RequestPart("foto") MultipartFile foto) {
        return service.atualizarTutor(id, lerDados(dados, TutorUpdateDTO.class), foto);
    }

    @PostMapping(value = "/pets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetResponseDTO> cadastrarPet(
            @RequestParam("dados") String dados, @RequestPart("foto") MultipartFile foto) {
        var resposta = service.cadastrarPet(lerDados(dados, PetRequestDTO.class), foto);
        return ResponseEntity.created(URI.create("/pets/" + resposta.id())).body(resposta);
    }

    @PutMapping(value = "/pets/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PetResponseDTO atualizarPet(@PathVariable("id") Long id,
                                       @RequestParam("dados") String dados, @RequestPart("foto") MultipartFile foto) {
        return service.atualizarPet(id, lerDados(dados, PetRequestDTO.class), foto);
    }

    @PostMapping(value = "/veterinarios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VeterinarioResponseDTO> cadastrarVeterinario(
            @RequestParam("dados") String dados, @RequestPart("foto") MultipartFile foto) {
        var resposta = service.cadastrarVeterinario(lerDados(dados, VeterinarioRequestDTO.class), foto);
        return ResponseEntity.created(URI.create("/veterinarios/" + resposta.id())).body(resposta);
    }

    @PutMapping(value = "/veterinarios/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VeterinarioResponseDTO atualizarVeterinario(@PathVariable("id") Long id,
                                                       @RequestParam("dados") String dados, @RequestPart("foto") MultipartFile foto) {
        return service.atualizarVeterinario(id, lerDados(dados, VeterinarioRequestDTO.class), foto);
    }

    private <T> T lerDados(String json, Class<T> tipo) {
        if (json.length() > 12_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do cadastro muito extensos.");
        }
        try {
            T dados = mapper.readValue(json, tipo);
            if (dados == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do cadastro.");
            var erros = validator.validate(dados);
            if (!erros.isEmpty()) {
                String mensagem = erros.stream().map(e -> e.getPropertyPath() + ": " + e.getMessage())
                        .sorted().collect(Collectors.joining("; "));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
            }
            return dados;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo dados deve conter um JSON válido.");
        }
    }
}