package com.barbearia.barbearia.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextNormalizer {

    private TextNormalizer() {}

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-|-$");


    // Remover acentos
    public static String removeAccents(String text) {
        if (text == null) return null;

        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return NON_ASCII.matcher(decomposed).replaceAll("");
    }

    // Converte para slug de URL: "Barbearia do Zé!" -> "barbearia-do-ze"
    public static String toSlug(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be empty to generate a slug.");
        }

        String slug = removeAccents(text)
                .toLowerCase(Locale.ROOT)
                .trim();

        slug = NON_ALPHANUMERIC.matcher(slug).replaceAll("-");
        slug = EDGE_HYPHENS.matcher(slug).replaceAll("-");

        if (slug.isEmpty()) {
            throw new IllegalArgumentException(
                    "It was not possible to generate a valid address from: " + text);
        }
        return slug;
    }

    // Colapsa espaços múltiplos: "João   da   Silva" → "João da Silva"
    public static String collapseSpaces(String text) {
        return text == null ? null : text.trim().replace("\\s+", " ");
    }

}
