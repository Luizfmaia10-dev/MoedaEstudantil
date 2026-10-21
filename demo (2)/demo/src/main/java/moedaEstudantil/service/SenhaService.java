package moedaEstudantil.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class SenhaService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String codificar(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        return encoder.encode(senha);
    }

    public boolean corresponde(String senhaInformada, String senhaArmazenada) {
        if (senhaInformada == null || senhaArmazenada == null) return false;
        return estaCodificada(senhaArmazenada)
                ? encoder.matches(senhaInformada, senhaArmazenada)
                : MessageDigest.isEqual(senhaArmazenada.getBytes(StandardCharsets.UTF_8),
                        senhaInformada.getBytes(StandardCharsets.UTF_8));
    }

    public boolean estaCodificada(String senha) {
        return senha != null && senha.matches("^\\$2[aby]\\$.*");
    }
}
