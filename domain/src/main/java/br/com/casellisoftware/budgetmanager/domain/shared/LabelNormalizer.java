package br.com.casellisoftware.budgetmanager.domain.shared;

import java.text.Normalizer;

/**
 * Utility for normalizing credit-card label strings so that SMS-extracted
 * card labels can be matched against user-supplied labels in a
 * case-, accent-, and whitespace-insensitive way.
 *
 * <p>Normalization steps:
 * <ol>
 *   <li>NFD decomposition (separates base characters from combining diacritics)</li>
 *   <li>Strip all combining diacritical marks (Unicode category Mn)</li>
 *   <li>Lowercase</li>
 *   <li>Strip leading/trailing whitespace</li>
 * </ol>
 *
 * <p>Example: {@code "BRADÉSCARD "} → {@code "bradescard"}
 *
 * @implNote Time complexity: O(n), Space complexity: O(n) where n = string length.
 */
public final class LabelNormalizer {

    private LabelNormalizer() {
    }

    /**
     * Normalizes a label string. Returns an empty string when {@code label} is null or blank.
     */
    public static String normalize(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(label, Normalizer.Form.NFD);
        String stripped = decomposed.replaceAll("\\p{Mn}", "");
        return stripped.toLowerCase().strip();
    }
}
