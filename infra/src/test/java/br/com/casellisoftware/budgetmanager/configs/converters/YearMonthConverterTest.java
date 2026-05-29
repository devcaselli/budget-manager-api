package br.com.casellisoftware.budgetmanager.configs.converters;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class YearMonthConverterTest {

    @Test
    void writeConverter_serializesYearMonthAsIsoString() {
        String result = new YearMonthWriteConverter().convert(YearMonth.of(2026, 5));

        assertThat(result).isEqualTo("2026-05");
    }

    @Test
    void writeConverter_serializesJanuaryWithZeroPadding() {
        String result = new YearMonthWriteConverter().convert(YearMonth.of(2026, 1));

        assertThat(result).isEqualTo("2026-01");
    }

    @Test
    void writeConverter_whenSourceIsNull_returnsNull() {
        assertThat(new YearMonthWriteConverter().convert(null)).isNull();
    }

    @Test
    void readConverter_deserializesIsoStringAsYearMonth() {
        YearMonth result = new YearMonthReadConverter().convert("2026-05");

        assertThat(result).isEqualTo(YearMonth.of(2026, 5));
    }

    @Test
    void readConverter_deserializesJanuaryWithZeroPadding() {
        YearMonth result = new YearMonthReadConverter().convert("2026-01");

        assertThat(result).isEqualTo(YearMonth.of(2026, 1));
    }

    @Test
    void readConverter_whenSourceIsNull_returnsNull() {
        assertThat(new YearMonthReadConverter().convert(null)).isNull();
    }
}
