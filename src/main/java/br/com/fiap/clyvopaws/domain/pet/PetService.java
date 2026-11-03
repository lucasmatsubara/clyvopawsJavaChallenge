package br.com.fiap.clyvopaws.domain.pet;

import br.com.fiap.clyvopaws.auth.AuthorizationService;
import br.com.fiap.clyvopaws.domain.tutor.Tutor;
import br.com.fiap.clyvopaws.domain.tutor.TutorRepository;
import br.com.fiap.clyvopaws.domain.tutor.TutorResumoDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetService {
    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public PetResponseDTO cadastrar(PetRequestDTO request) {
        authorizationService.assertSelfTutor(request.tutorId());
        Tutor tutor = tutorRepository.findById(request.tutorId()).orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado."));
        Pet pet = new Pet();
        pet.setNome(request.nome());
        pet.setEspecie(request.especie());
        pet.setRaca(request.raca());
        pet.setPeso(request.peso());
        pet.setSexo(request.sexo());
        pet.setDataNascimento(request.dataNascimento());
        pet.setDescricao(request.descricao());
        pet.setFotoUrl(request.fotoUrl());
        pet.setTutor(tutor);
        return toResponseDTO(petRepository.save(pet));
    }

    @Transactional(readOnly = true)
    public PetResponseDTO buscarPorId(Long id) {
        Pet pet = petRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Pet não encontrado."));
        if (!authorizationService.hasRole("VETERINARIO")) {
            authorizationService.assertSelfTutor(pet.getTutor().getId());
        }
        return toResponseDTO(pet);
    }

    @Transactional(readOnly = true)
    public List<PetResponseDTO> listarPorTutor(Long tutorId) {
        if (!authorizationService.hasRole("VETERINARIO")) {
            authorizationService.assertSelfTutor(tutorId);
        }
        return petRepository.findByTutorId(tutorId).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PetResponseDTO> listarTodosPaginado(Pageable pageable) {
        if (!authorizationService.hasRole("VETERINARIO") && !authorizationService.isAdmin()) {
            throw new AccessDeniedException("Apenas veterinários ou administradores podem listar todos os pets.");
        }
        return petRepository.findAll(pageable).map(this::toResponseDTO);
    }

    @Transactional
    public PetResponseDTO atualizar(Long id, PetRequestDTO request) {
        Pet pet = petRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Pet não encontrado."));
        authorizationService.assertSelfTutor(pet.getTutor().getId());
        pet.setNome(request.nome());
        pet.setEspecie(request.especie());
        pet.setRaca(request.raca());
        pet.setPeso(request.peso());
        pet.setSexo(request.sexo());
        pet.setDataNascimento(request.dataNascimento());
        pet.setDescricao(request.descricao());
        // Só altera a foto quando uma URL for enviada; um PUT sem fotoUrl mantém a atual.
        if (request.fotoUrl() != null) {
            pet.setFotoUrl(request.fotoUrl());
        }

        return toResponseDTO(petRepository.save(pet));
    }

    @Transactional
    public void excluir(Long id) {
        Pet pet = petRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Pet não encontrado."));
        authorizationService.assertSelfTutor(pet.getTutor().getId());
        petRepository.deleteById(id);
    }

    public PetResponseDTO toResponseDTO(Pet pet) {
        Tutor t = pet.getTutor();
        TutorResumoDTO tutorDTO = new TutorResumoDTO(t.getId(), t.getNomeCompleto());

        return new PetResponseDTO(pet.getId(), pet.getNome(), pet.getEspecie(), pet.getRaca(), pet.getPeso(), pet.getSexo(), pet.getDataNascimento(), pet.getDescricao(), pet.getFotoUrl(), tutorDTO);
    }
}