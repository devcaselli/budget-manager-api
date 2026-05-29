package br.com.casellisoftware.budgetmanager.configs.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.YearMonth;

@ReadingConverter
public class YearMonthReadConverter implements Converter<String, YearMonth> {

    @Override
    public YearMonth convert(String source) {
        if (source == null) {
            return null;
        }
        return YearMonth.parse(source);
    }
}
