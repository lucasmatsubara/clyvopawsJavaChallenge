package br.com.fiap.clyvopaws.domain.foto;

import br.com.fiap.clyvopaws.auth.AuthorizationService;
import br.com.fiap.clyvopaws.domain.pet.*;
import br.com.fiap.clyvopaws.domain.tutor.*;
import br.com.fiap.clyvopaws.domain.veterinario.*;
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
    private final VeterinarioService veterinarioService;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AuthorizationService authorizationService;
    private final FotoStorageService fotoStorage;

    private boolean temFoto(MultipartFile foto) {
        return foto != null && !foto.isEmpty();
    }

    @Transactional
    public TutorResponseDTO cadastrarTutor(TutorRequestDTO dados, MultipartFile foto) {
        if (!temFoto(foto)) {
            return tutorService.cadastrar(dados);
        }
        var semFoto = new TutorRequestDTO(dados.nomeCompleto(), dados.cpf(), dados.telefone(),
                dados.email(), dados.senha(), null, dados.endereco());
        var resposta = tutorService.cadastrar(semFoto);
        var tutor = tutorRepository.findById(resposta.id()).orElseThrow();
        tutor.setFotoUrl(fotoStorage.salvarTutor(foto, tutor.getId()));
        return new TutorResponseDTO(tutor);
    }

    @Transactional
    public TutorResponseDTO atualizarTutor(Long id, TutorUpdateDTO dados, MultipartFile foto) {
        if (!temFoto(foto)) {
            return tutorService.atualizar(id, dados);
        }
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
        if (!temFoto(foto)) {
            return petService.cadastrar(dados);
        }
        // O serviço existente valida a propriedade do tutor antes de qualquer envio.
        var resposta = petService.cadastrar(comUrl(dados, null));
        var pet = petRepository.findById(resposta.id()).orElseThrow();
        pet.setFotoUrl(fotoStorage.salvarPet(foto, pet.getId()));
        return petService.toResponseDTO(pet);
    }

    @Transactional
    public PetResponseDTO atualizarPet(Long id, PetRequestDTO dados, MultipartFile foto) {
        if (!temFoto(foto)) {
            return petService.atualizar(id, dados);
        }
        var pet = petRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pet não encontrado."));
        authorizationService.assertSelfTutor(pet.getTutor().getId());
        String url = fotoStorage.salvarPet(foto, id);
        return petService.atualizar(id, comUrl(dados, url));
    }

    @Transactional
    public VeterinarioResponseDTO cadastrarVeterinario(VeterinarioRequestDTO dados, MultipartFile foto) {
        if (!temFoto(foto)) {
            return veterinarioService.cadastrar(dados);
        }
        var resposta = veterinarioService.cadastrar(comUrl(dados, null));
        var vet = veterinarioRepository.findById(resposta.id()).orElseThrow();
        vet.setFotoUrl(fotoStorage.salvarVeterinario(foto, vet.getId()));
        return new VeterinarioResponseDTO(vet);
    }

    @Transactional
    public VeterinarioResponseDTO atualizarVeterinario(Long id, VeterinarioRequestDTO dados, MultipartFile foto) {
        if (!temFoto(foto)) {
            return veterinarioService.atualizar(id, dados);
        }
        authorizationService.assertSelfVeterinario(id);
        if (!veterinarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Veterinário não encontrado.");
        }
        String url = fotoStorage.salvarVeterinario(foto, id);
        return veterinarioService.atualizar(id, comUrl(dados, url));
    }

    private PetRequestDTO comUrl(PetRequestDTO d, String url) {
        return new PetRequestDTO(d.nome(), d.especie(), d.raca(), d.peso(), d.sexo(),
                d.dataNascimento(), d.descricao(), url, d.tutorId());
    }

    private VeterinarioRequestDTO comUrl(VeterinarioRequestDTO d, String url) {
        return new VeterinarioRequestDTO(d.nomeCompleto(), d.email(), d.telefone(), url,
                d.crmv(), d.clinicaId(), d.username(), d.password());
    }
}