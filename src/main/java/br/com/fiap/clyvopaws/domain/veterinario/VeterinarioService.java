package br.com.fiap.clyvopaws.domain.veterinario;

import br.com.fiap.clyvopaws.auth.AuthorizationService;
import br.com.fiap.clyvopaws.domain.clinica.ClinicaService;
import br.com.fiap.clyvopaws.domain.consulta.ConsultaRepository;
import br.com.fiap.clyvopaws.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final ClinicaService clinicaService;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final ConsultaRepository consultaRepository;

    @Transactional
    public VeterinarioResponseDTO cadastrar(VeterinarioRequestDTO dto) {
        var clinica = clinicaService.buscarPorId(dto.clinicaId());

        var user = new User();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole("VETERINARIO");

        var vet = new Veterinario();
        vet.setNomeCompleto(dto.nomeCompleto());
        vet.setEmail(dto.email());
        vet.setTelefone(dto.telefone());
        vet.setFotoUrl(dto.fotoUrl());
        vet.setCrmv(dto.crmv());
        vet.setClinica(clinica);
        vet.setUser(user);

        return new VeterinarioResponseDTO(veterinarioRepository.save(vet));
    }

    @Transactional(readOnly = true)
    public VeterinarioResponseDTO buscarDtoPorId(Long id) {
        var vet = veterinarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Veterinário não encontrado."));
        return new VeterinarioResponseDTO(vet);
    }

    @Transactional(readOnly = true)
    public Page<VeterinarioResponseDTO> listarTodos(Pageable pageable) {
        return veterinarioRepository.findAll(pageable).map(VeterinarioResponseDTO::new);
    }

    @Transactional
    public VeterinarioResponseDTO atualizar(Long id, VeterinarioRequestDTO dto) {
        authorizationService.assertSelfVeterinario(id);
        var vet = veterinarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Veterinário não encontrado."));

        var clinica = clinicaService.buscarPorId(dto.clinicaId());

        vet.setNomeCompleto(dto.nomeCompleto());
        vet.setEmail(dto.email());
        vet.setTelefone(dto.telefone());
        // Só altera a foto quando uma URL for enviada; um PUT sem fotoUrl mantém a atual.
        if (dto.fotoUrl() != null) {
            vet.setFotoUrl(dto.fotoUrl());
        }
        vet.setCrmv(dto.crmv());
        vet.setClinica(clinica);

        return new VeterinarioResponseDTO(veterinarioRepository.save(vet));
    }

    @Transactional
    public void excluir(Long id) {
        authorizationService.assertSelfVeterinario(id);
        if (!veterinarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Veterinário não encontrado.");
        }
        if (consultaRepository.existsByVeterinarioId(id)) {
            throw new IllegalArgumentException(
                    "Não é possível excluir: este veterinário possui consultas registradas.");
        }
        veterinarioRepository.deleteById(id);
    }
}