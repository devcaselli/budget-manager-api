package br.com.casellisoftware.budgetmanager.persistence.sync;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(SyncPreferenceRepositoryImpl.class)
class SyncPreferenceRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private SyncPreferenceRepositoryImpl repository;

    // ---------- save / findByOwnerId ----------

    @Test
    void save_thenFindByOwnerId_roundtripsAllFields() {
        SyncPreference saved = repository.save(SyncPreference.defaultFor("owner-1"));

        Optional<SyncPreference> result = repository.findByOwnerId("owner-1");

        assertThat(result).isPresent();
        assertThat(result.get().getOwnerId()).isEqualTo("owner-1");
        assertThat(result.get().isEnabled()).isTrue();
    }

    @Test
    void save_withEnabledFalse_persistsCorrectly() {
        repository.save(SyncPreference.defaultFor("owner-2").withEnabled(false));

        Optional<SyncPreference> result = repository.findByOwnerId("owner-2");

        assertThat(result).isPresent();
        assertThat(result.get().isEnabled()).isFalse();
    }

    @Test
    void save_updateExisting_overwritesDocument() {
        repository.save(SyncPreference.defaultFor("owner-3"));
        repository.save(SyncPreference.defaultFor("owner-3").withEnabled(false));

        Optional<SyncPreference> result = repository.findByOwnerId("owner-3");

        assertThat(result).isPresent();
        assertThat(result.get().isEnabled()).isFalse();
    }

    @Test
    void findByOwnerId_whenMissing_returnsEmpty() {
        Optional<SyncPreference> result = repository.findByOwnerId("nonexistent");

        assertThat(result).isEmpty();
    }

    // ---------- findAllEnabledOwnerIds ----------

    @Test
    void findAllEnabledOwnerIds_returnsOnlyEnabledOwners() {
        repository.save(SyncPreference.defaultFor("enabled-1"));
        repository.save(SyncPreference.defaultFor("enabled-2"));
        repository.save(SyncPreference.defaultFor("disabled-1").withEnabled(false));

        List<String> ids = repository.findAllEnabledOwnerIds();

        assertThat(ids).containsExactlyInAnyOrder("enabled-1", "enabled-2");
        assertThat(ids).doesNotContain("disabled-1");
    }

    @Test
    void findAllEnabledOwnerIds_whenNoneEnabled_returnsEmptyList() {
        repository.save(SyncPreference.defaultFor("owner-a").withEnabled(false));
        repository.save(SyncPreference.defaultFor("owner-b").withEnabled(false));

        List<String> ids = repository.findAllEnabledOwnerIds();

        assertThat(ids).isEmpty();
    }

    @Test
    void findAllEnabledOwnerIds_whenNoPersisted_returnsEmptyList() {
        assertThat(repository.findAllEnabledOwnerIds()).isEmpty();
    }
}
