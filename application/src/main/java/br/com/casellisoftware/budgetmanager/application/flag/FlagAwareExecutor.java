package br.com.casellisoftware.budgetmanager.application.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class FlagAwareExecutor<I, O> {

    private static final Logger log = LoggerFactory.getLogger(FlagAwareExecutor.class);

    private final FlagManager flagManager;
    private final FlagStrategyRegistry<I, O> registry;

    public FlagAwareExecutor(FlagManager flagManager, FlagStrategyRegistry<I, O> registry) {
        this.flagManager = Objects.requireNonNull(flagManager, "flagManager must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    public O execute(FlagEnum flag, I input) {
        boolean enabled = flagManager.isEnabled(flag);
        FlagEnum effectiveFlag = enabled ? flag : FlagEnum.NONE;
        FlagStrategy<I, O> strategy = registry.resolve(effectiveFlag);
        log.info("flag.dispatch flag={} enabled={} strategy={}",
                flag, enabled, strategy.getClass().getSimpleName());
        return strategy.apply(input);
    }
}
