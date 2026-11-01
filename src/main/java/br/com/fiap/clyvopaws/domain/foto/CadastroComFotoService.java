package br.com.fiap.clyvopaws.domain.foto;

import br.com.fiap.clyvopaws.auth.AuthorizationService;
import br.com.fiap.clyvopaws.domain.pet.*;
import br.com.fiap.clyvopaws.domain.tutor.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CadastroComFotoService {
    private final TutorService tutorService;
    private final PetService petService;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final AuthorizationService authorizationService;
    private final FotoStorageService fotoStorage;

    @Transactional
    public TutorResponseDTO cadastrarTutor(TutorRequestDTO dados, MultipartFile foto) {
        // A conta e o BLOB usam a mesma transação: qualquer falha desfaz as duas gravações.
        var semFoto = new TutorRequestDTO(dados.nomeCompleto(), dados.cpf(), dados.telefone(),
                dados.email(), dados.senha(), null, dados.endereco());
        var resposta = tutorService.cadastrar(semFoto);
        var tutor = tutorRepository.findById(resposta.id()).orElseThrow();
        tutor.setFotoUrl(fotoStorage.salvarTutor(foto, tutor.getId()));
        return new TutorResponseDTO(tutor);
    }

    @Transactional
    public TutorResponseDTO atualizarTutor(Long id, TutorUpdateDTO dados, MultipartFile foto) {
        authorizationService.assertSelfTutor(id);
        var tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado."));
        String url = fotoStorage.salvarTutor(foto, id);
        tutorService.atualizar(id, new TutorUpdateDTO(dados.nomeCompleto(), dados.email(),
                dados.cpf(), dados.telefone(), url));
        return new TutorResponseDTO(tutor);
    }

    @Transactional
    public PetResponseDTO cadastrarPet(PetRequestDTO dados, MultipartFile foto) {
        // O serviço existente valida a propriedade do tutor antes de qualquer envio.
        var resposta = petService.cadastrar(comUrl(dados, null));
        var pet = petRepository.findById(resposta.id()).orElseThrow();
        pet.setFotoUrl(fotoStorage.salvarPet(foto, pet.getId()));
        return petService.toResponseDTO(pet);
    }

    @Transactional
    public PetResponseDTO atualizarPet(Long id, PetRequestDTO dados, MultipartFile foto) {
        var pet = petRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pet não encontrado."));
        authorizationService.assertSelfTutor(pet.getTutor().getId());
        String url = fotoStorage.salvarPet(foto, id);
        var resposta = petService.atualizar(id, comUrl(dados, url));
        return resposta;
    }

    private PetRequestDTO comUrl(PetRequestDTO d, String url) {
        return new PetRequestDTO(d.nome(), d.especie(), d.raca(), d.peso(), d.sexo(),
                d.dataNascimento(), d.descricao(), url, d.tutorId());
    }
}