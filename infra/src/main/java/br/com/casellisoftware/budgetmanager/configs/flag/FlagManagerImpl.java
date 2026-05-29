package br.com.casellisoftware.budgetmanager.configs.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;

import java.util.Objects;

public final class FlagManagerImpl implements FlagManager {

    private final FlagsProperties properties;

    public FlagManagerImpl(FlagsProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public boolean isEnabled(FlagEnum flag) {
        if (flag == null || flag == FlagEnum.NONE) {
            return false;
        }
        return Boolean.TRUE.equals(properties.getValues().get(flag));
    }
}
