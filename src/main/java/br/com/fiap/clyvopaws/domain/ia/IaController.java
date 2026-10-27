package br.com.fiap.clyvopaws.domain.ia;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ia")
public class IaController {

    private final IaService iaService;

    public IaController(IaService iaService) {
        this.iaService = iaService;
    }

    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    @PostMapping(
            value = "/analisar-pet",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AnalisePetResponseDTO analisarPet(
            @RequestPart("foto") MultipartFile foto) {

        return iaService.analisar(foto);
    }
}