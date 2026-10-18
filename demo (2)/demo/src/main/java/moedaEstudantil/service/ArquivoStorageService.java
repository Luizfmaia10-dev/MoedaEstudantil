package moedaEstudantil.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Armazena imagens enviadas pelas empresas em disco.
 * O nome do arquivo e gerado pelo servidor (UUID) e a extensao deriva do tipo
 * de conteudo permitido, nunca do nome enviado pelo cliente (evita path traversal).
 */
@Service
public class ArquivoStorageService {

    public static final String URL_PREFIX = "/uploads/";

    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path diretorio;

    public ArquivoStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.diretorio = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.diretorio);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel criar o diretorio de uploads", e);
        }
    }

    public Path getDiretorio() {
        return diretorio;
    }

    /** Salva a imagem e retorna a URL publica relativa (ex.: /uploads/abc.png). */
    public String salvarImagem(MultipartFile arquivo) {
        String extensao = TIPOS_PERMITIDOS.get(arquivo.getContentType());
        if (extensao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de imagem invalido. Use JPG, PNG, WEBP ou GIF.");
        }
        String nome = UUID.randomUUID() + extensao;
        try {
            Files.copy(arquivo.getInputStream(), diretorio.resolve(nome));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao salvar a imagem.");
        }
        return URL_PREFIX + nome;
    }

    /** Remove o arquivo associado a uma URL gerada por este servico (ignora URLs desconhecidas). */
    public void remover(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        Path alvo = diretorio.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!alvo.startsWith(diretorio)) return;
        try {
            Files.deleteIfExists(alvo);
        } catch (IOException ignored) {
            // limpeza best-effort
        }
    }
}
