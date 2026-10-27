package br.com.fiap.clyvopaws.domain.ia;

import java.util.List;

public record AnalisePetResponseDTO(
        String status,
        String especie,
        String racaProvavel,
        String confiancaVisual,
        List<String> caracteristicas,
        String motivo,
        String idAnalise,
        String modelo,
        Boolean requerConfirmacaoTutor,
        String aviso
) {}