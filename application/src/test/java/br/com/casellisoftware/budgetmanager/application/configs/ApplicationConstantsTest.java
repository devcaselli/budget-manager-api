package br.com.casellisoftware.budgetmanager.application.configs;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConstantsTest {

    @Test
    void classIsFinal() {
        assertThat(Modifier.isFinal(ApplicationConstants.class.getModifiers())).isTrue();
    }

    @Test
    void constructorIsPrivate() throws NoSuchMethodException {
        Constructor<ApplicationConstants> constructor = ApplicationConstants.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void constantsAreDefined() {
        assertThat(ApplicationConstants.SAVE_EXPENSE_START).contains("walletId");
        assertThat(ApplicationConstants.SAVE_EXPENSE_SUCCESS).contains("id");
    }
}
