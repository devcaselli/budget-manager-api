package br.com.casellisoftware.budgetmanager.configs.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.YearMonth;

@WritingConverter
public class YearMonthWriteConverter implements Converter<YearMonth, String> {

    @Override
    public String convert(YearMonth source) {
        if (source == null) {
            return null;
        }
        return source.toString();
    }
}
