package br.com.casellisoftware.budgetmanager.domain.sync;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SyncPreferenceTest {

    @Test
    void constructor_nullOwnerId_throwsNPE() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SyncPreference(null, true))
                .withMessage("ownerId must not be null");
    }

    @Test
    void constructor_blankOwnerId_throwsIllegalArgument() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SyncPreference("  ", true))
                .withMessage("ownerId must not be blank");
    }

    @Test
    void defaultFor_createsEnabledPreference() {
        SyncPreference pref = SyncPreference.defaultFor("owner-1");
        assertThat(pref.getOwnerId()).isEqualTo("owner-1");
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    void withEnabled_false_returnsDisabledInstance() {
        SyncPreference pref = SyncPreference.defaultFor("owner-1");
        SyncPreference disabled = pref.withEnabled(false);
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.getOwnerId()).isEqualTo("owner-1");
    }

    @Test
    void withEnabled_doesNotMutateOriginal() {
        SyncPreference original = SyncPreference.defaultFor("owner-1");
        original.withEnabled(false);
        assertThat(original.isEnabled()).isTrue();
    }

    @Test
    void equals_sameOwnerId_equal() {
        SyncPreference a = new SyncPreference("owner-1", true);
        SyncPreference b = new SyncPreference("owner-1", false);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentOwnerId_notEqual() {
        assertThat(new SyncPreference("owner-1", true)).isNotEqualTo(new SyncPreference("owner-2", true));
    }

    @Test
    void hashCode_sameOwnerId_sameHashCode() {
        assertThat(new SyncPreference("owner-1", true).hashCode())
                .isEqualTo(new SyncPreference("owner-1", false).hashCode());
    }
}
