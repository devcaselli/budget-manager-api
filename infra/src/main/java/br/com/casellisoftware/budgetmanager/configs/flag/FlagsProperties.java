package br.com.casellisoftware.budgetmanager.configs.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties(prefix = "flags")
public class FlagsProperties {

    private Map<FlagEnum, Boolean> values = new EnumMap<>(FlagEnum.class);

    public Map<FlagEnum, Boolean> getValues() {
        return values;
    }

    public void setValues(Map<FlagEnum, Boolean> values) {
        this.values = values == null ? new EnumMap<>(FlagEnum.class) : values;
    }
}
