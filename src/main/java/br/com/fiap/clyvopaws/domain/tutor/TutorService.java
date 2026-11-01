package br.com.fiap.clyvopaws.domain.tutor;

import br.com.fiap.clyvopaws.auth.AuthorizationService;
import br.com.fiap.clyvopaws.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.clyvopaws.domain.user.UserRepository;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;

    @Transactional
    public TutorResponseDTO cadastrar(TutorRequestDTO request) {
        if (tutorRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
        if (tutorRepository.existsByCpf(request.cpf())) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }

        var user = new User();
        user.setUsername(request.email());
        user.setPassword(passwordEncoder.encode(request.senha()));
        user.setRole("TUTOR");

        Tutor tutor = new Tutor();
        tutor.setNomeCompleto(request.nomeCompleto());
        tutor.setCpf(request.cpf());
        tutor.setTelefone(request.telefone());
        tutor.setEmail(request.email());
        tutor.setFotoUrl(request.fotoUrl());
        tutor.setUser(user);

        if (request.endereco() != null) {
            Endereco endereco = new Endereco();
            endereco.setRua(request.endereco().rua());
            endereco.setNumero(request.endereco().numero());
            endereco.setComplemento(request.endereco().complemento());
            endereco.setBairro(request.endereco().bairro());
            endereco.setCep(request.endereco().cep());
            endereco.setCidade(request.endereco().cidade());
            endereco.setEstado(request.endereco().estado());
            tutor.setEndereco(endereco);
        }

        return toResponseDTO(tutorRepository.save(tutor));
    }

    @Transactional(readOnly = true)
    public TutorResponseDTO buscarPorId(Long id) {
        if (!authorizationService.hasRole("VETERINARIO")) {
            authorizationService.assertSelfTutor(id);
        }
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado."));
        return toResponseDTO(tutor);
    }

    @Transactional(readOnly = true)
    public Page<TutorResponseDTO> listarTodos(Pageable pageable) {
        if (!authorizationService.hasRole("VETERINARIO") && !authorizationService.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Apenas veterinários ou administradores podem listar todos os tutores.");
        }
        return tutorRepository.findAll(pageable).map(this::toResponseDTO);
    }

    @Transactional
    public TutorResponseDTO atualizar(Long id, TutorUpdateDTO dto) {
        authorizationService.assertSelfTutor(id);

        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado com o id: " + id));

        tutor.setNomeCompleto(dto.nomeCompleto());
        tutor.setEmail(dto.email());
        tutor.setCpf(dto.cpf());
        tutor.setTelefone(dto.telefone());

        if (dto.foto() != null) {
            tutor.setFotoUrl(dto.foto());
        }

        if (tutor.getUser() != null) {
            tutor.getUser().setUsername(dto.email());
            userRepository.save(tutor.getUser());
        }

        return toResponseDTO(tutorRepository.save(tutor));
    }

    @Transactional
    public void excluir(Long id) {
        authorizationService.assertSelfTutor(id);

        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado."));

        User user = tutor.getUser();

        tutorRepository.delete(tutor);

        if (user != null) {
            userRepository.delete(user);
        }
    }

    private TutorResponseDTO toResponseDTO(Tutor tutor) {
        return new TutorResponseDTO(tutor);
    }


}