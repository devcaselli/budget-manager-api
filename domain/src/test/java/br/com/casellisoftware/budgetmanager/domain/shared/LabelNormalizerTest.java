package br.com.casellisoftware.budgetmanager.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LabelNormalizerTest {

    @Test
    void normalize_nullInput_returnsEmpty() {
        assertThat(LabelNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void normalize_blankInput_returnsEmpty() {
        assertThat(LabelNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void normalize_emptyString_returnsEmpty() {
        assertThat(LabelNormalizer.normalize("")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "BRADÉSCARD , bradescard",
        "bradéscard,  bradescard",
        "BRADESCARD,  bradescard",
        "  Bradescard  , bradescard"
    })
    void normalize_accentAndCaseVariants_returnsSameResult(String input, String expected) {
        assertThat(LabelNormalizer.normalize(input)).isEqualTo(expected.strip());
    }

    @Test
    void normalize_multipleAccents_allStripped() {
        assertThat(LabelNormalizer.normalize("Açúcar")).isEqualTo("acucar");
    }

    @Test
    void normalize_alreadyNormalized_returnsUnchanged() {
        assertThat(LabelNormalizer.normalize("nubank")).isEqualTo("nubank");
    }

    @Test
    void normalize_leadingTrailingWhitespace_stripped() {
        assertThat(LabelNormalizer.normalize("  nubank  ")).isEqualTo("nubank");
    }

    @Test
    void normalize_uppercaseNoAccents_lowercased() {
        assertThat(LabelNormalizer.normalize("NUBANK")).isEqualTo("nubank");
    }
}
