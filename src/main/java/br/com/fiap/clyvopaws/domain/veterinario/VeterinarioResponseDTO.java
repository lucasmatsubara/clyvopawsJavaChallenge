package br.com.fiap.clyvopaws.domain.veterinario;

public record VeterinarioResponseDTO(
        Long id,
        String nomeCompleto,
        String crmv,
        String email,
        String clinicaNome,
        String fotoUrl
) {
    public VeterinarioResponseDTO(Veterinario vet) {
        this(
                vet.getId(),
                vet.getNomeCompleto(),
                vet.getCrmv(),
                vet.getEmail(),
                vet.getClinica() != null ? vet.getClinica().getNome() : null,
                vet.getFotoUrl()
        );
    }
}