package br.com.casellisoftware.budgetmanager.domain.shared;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResultTest {

    @Test
    void constructor_validArgs_createsImmutablePage() {
        PageResult<String> page = new PageResult<>(List.of("a", "b"), 0, 10, 2, 1);

        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void constructor_defensivelyCopiesContent() {
        List<String> mutable = new ArrayList<>(List.of("a"));
        PageResult<String> page = new PageResult<>(mutable, 0, 10, 1, 1);

        mutable.add("b");

        assertThat(page.content()).containsExactly("a");
    }

    @Test
    void constructor_contentIsUnmodifiable() {
        PageResult<String> page = new PageResult<>(List.of("a"), 0, 10, 1, 1);

        assertThatThrownBy(() -> page.content().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_nullContent_throws() {
        assertThatThrownBy(() -> new PageResult<>(null, 0, 10, 0, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("content");
    }

    @Test
    void constructor_negativePage_throws() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), -1, 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    void constructor_zeroSize_throws() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void constructor_negativeTotalElements_throws() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 10, -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalElements");
    }

    @Test
    void constructor_negativeTotalPages_throws() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 10, 0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalPages");
    }

    @Test
    void emptyPage_hasZeroTotals() {
        PageResult<String> page = new PageResult<>(List.of(), 0, 10, 0, 0);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }
}
