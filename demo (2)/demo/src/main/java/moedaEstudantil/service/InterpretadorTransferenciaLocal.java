package moedaEstudantil.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InterpretadorTransferenciaLocal implements InterpretadorTransferenciaIA {

    private static final Pattern NUMERO = Pattern.compile("(?<!\\p{L})(\\d+)(?!\\p{L})");
    private static final Pattern DESTINATARIO = Pattern.compile(
            "(?iu)\\b(?:para|pra|pro)\\s+(.+?)(?=\\s+(?:pelo|pela|com\\s+(?:o|a)?\\s*professor(?:a)?|porque|pois)\\b|[.,;!?]|$)");
    private static final Pattern PROFESSOR = Pattern.compile(
            "(?iu)\\b(?:(?:pelo|pela|com\\s+(?:o|a))\\s+)?professor(?:a)?\\s+(.+?)(?=\\s+(?:porque|pois)\\b|[.,;!?]|$)");
    private static final Pattern JUSTIFICATIVA = Pattern.compile("(?iu)\\b(?:porque|pois)\\s+(.+?)(?:[.!?]|$)");
    private static final Pattern PREFIXO_PESSOA = Pattern.compile(
            "(?iu)^(?:(?:o|a|um|uma)\\s+)?(?:(?:aluno|aluna|colega)\\s+)?");
    private static final Set<String> DESTINATARIOS_GENERICOS = Set.of(
            "", "aluno", "aluna", "colega", "um aluno", "uma aluna", "um colega", "uma colega", "pessoa");
    private static final Map<String, Integer> UNIDADES = Map.ofEntries(
            Map.entry("zero", 0), Map.entry("um", 1), Map.entry("uma", 1),
            Map.entry("dois", 2), Map.entry("duas", 2), Map.entry("tres", 3),
            Map.entry("quatro", 4), Map.entry("cinco", 5), Map.entry("seis", 6),
            Map.entry("sete", 7), Map.entry("oito", 8), Map.entry("nove", 9),
            Map.entry("dez", 10), Map.entry("onze", 11), Map.entry("doze", 12),
            Map.entry("treze", 13), Map.entry("quatorze", 14), Map.entry("catorze", 14),
            Map.entry("quinze", 15), Map.entry("dezesseis", 16), Map.entry("dezessete", 17),
            Map.entry("dezoito", 18), Map.entry("dezenove", 19));
    private static final Map<String, Integer> DEZENAS = Map.of(
            "vinte", 20, "trinta", 30, "quarenta", 40, "cinquenta", 50,
            "sessenta", 60, "setenta", 70, "oitenta", 80, "noventa", 90);

    @Override
    public Intencao interpretar(String mensagem, Rascunho rascunho) {
        Rascunho atual = rascunho == null ? new Rascunho(null, null, null, null) : rascunho;
        String texto = mensagem == null ? "" : mensagem.trim();

        String destinatario = extrairPessoa(DESTINATARIO, texto, true);
        String professor = extrairPessoa(PROFESSOR, texto, false);
        Integer quantidade = extrairQuantidade(texto);
        String justificativa = extrairGrupo(JUSTIFICATIVA, texto);

        return new Intencao(
                destinatario != null ? destinatario : atual.destinatario(),
                professor != null ? professor : atual.professor(),
                quantidade != null ? quantidade : atual.quantidade(),
                justificativa != null ? justificativa : atual.justificativa());
    }

    private Integer extrairQuantidade(String texto) {
        String normalizado = normalizar(texto);
        Matcher algarismo = NUMERO.matcher(normalizado);
        if (algarismo.find()) {
            return Integer.valueOf(algarismo.group(1));
        }

        String[] palavras = normalizado.split("[^a-z0-9]+");
        for (int moeda = 0; moeda < palavras.length; moeda++) {
            if (!palavras[moeda].startsWith("moeda")) {
                continue;
            }
            for (int inicio = Math.max(0, moeda - 4); inicio < moeda; inicio++) {
                Integer valor = numeroPorExtenso(Arrays.copyOfRange(palavras, inicio, moeda));
                if (valor != null) {
                    return valor;
                }
            }
        }
        return null;
    }

    private Integer numeroPorExtenso(String[] palavras) {
        int total = 0;
        boolean encontrou = false;
        for (String palavra : palavras) {
            if (palavra.equals("e")) {
                continue;
            }
            Integer valor = UNIDADES.get(palavra);
            if (valor == null) {
                valor = DEZENAS.get(palavra);
            }
            if (valor == null) {
                return null;
            }
            total += valor;
            encontrou = true;
        }
        return encontrou ? total : null;
    }

    private String extrairPessoa(Pattern pattern, String texto, boolean validarGenerico) {
        String pessoa = extrairGrupo(pattern, texto);
        if (pessoa == null) {
            return null;
        }
        pessoa = PREFIXO_PESSOA.matcher(pessoa).replaceFirst("").trim();
        if (validarGenerico && DESTINATARIOS_GENERICOS.contains(normalizar(pessoa))) {
            return null;
        }
        return pessoa.isBlank() ? null : pessoa;
    }

    private String extrairGrupo(Pattern pattern, String texto) {
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
