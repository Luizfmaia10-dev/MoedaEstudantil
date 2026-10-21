package moedaEstudantil.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
                : senhaArmazenada.equals(senhaInformada);
    }

    public boolean estaCodificada(String senha) {
        return senha != null && senha.matches("^\\$2[aby]\\$\\d{2}\\$.*");
    }
}
