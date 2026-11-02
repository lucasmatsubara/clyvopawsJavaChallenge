package br.com.fiap.clyvopaws.domain.foto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Fotos", description = "Leitura pública das fotos de tutores e pets armazenadas no banco")
public class LeituraFotoController {
    private final FotoStorageService fotoStorage;

    @GetMapping(value = "/fotos/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> buscar(@PathVariable("id") String id) {
        byte[] bytes = fotoStorage.buscar(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                .contentLength(bytes.length).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=\"foto.jpg\"")
                .body(bytes);
    }
}