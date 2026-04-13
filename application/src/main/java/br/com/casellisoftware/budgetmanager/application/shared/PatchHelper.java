package br.com.casellisoftware.budgetmanager.application.shared;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Generic patch helper that merges non-null fields from a patch input into an
 * existing immutable domain entity, producing a new instance via reflection.
 *
 * <p><b>How it works:</b></p>
 * <ol>
 *   <li>Reads every instance field of the existing entity.</li>
 *   <li>For each field, looks for a same-named accessor on the patch input (record-style).</li>
 *   <li>If the patch value is non-null, uses it (applying type conversion when needed);
 *       otherwise keeps the existing value.</li>
 *   <li>Invokes the entity constructor whose parameter count matches the field count.</li>
 * </ol>
 *
 * <p><b>Type conversions supported:</b> {@code BigDecimal → Money}.</p>
 *
 * <p><b>Contract:</b> the entity's constructor parameter order must match its
 * declared field order, and the patch input's field names must match the entity
 * field names exactly.</p>
 */
public final class PatchHelper {

    private PatchHelper() {
    }

    /**
     * Applies non-null fields from {@code patchInput} onto {@code existing},
     * returning a new instance of {@code T}.
     *
     * @param existing   the current domain entity (never null)
     * @param patchInput a record whose non-null fields override the entity values
     * @param <T>        the domain entity type
     * @return a new entity instance with merged values
     */
    @SuppressWarnings("unchecked")
    public static <T> T applyPatch(T existing, Object patchInput) {
        Class<?> entityClass = existing.getClass();

        List<Field> entityFields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .toList();

        Object[] constructorArgs = new Object[entityFields.size()];

        for (int i = 0; i < entityFields.size(); i++) {
            Field entityField = entityFields.get(i);
            entityField.setAccessible(true);

            Object existingValue = readField(entityField, existing);
            Object patchValue = readAccessor(patchInput, entityField.getName());

            if (patchValue != null) {
                constructorArgs[i] = convertIfNeeded(patchValue, entityField.getType());
            } else {
                constructorArgs[i] = existingValue;
            }
        }

        Constructor<?> constructor = findConstructor(entityClass, entityFields.size());
        constructor.setAccessible(true);

        try {
            return (T) constructor.newInstance(constructorArgs);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create patched instance of " + entityClass.getSimpleName(), e);
        }
    }

    private static Object readField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read field: " + field.getName(), e);
        }
    }

    /**
     * Reads a value from the patch input using its record-style accessor
     * (method with the same name as the field, no parameters).
     * Returns {@code null} if the accessor does not exist or its value is null.
     */
    private static Object readAccessor(Object patchInput, String fieldName) {
        try {
            var method = patchInput.getClass().getMethod(fieldName);
            return method.invoke(patchInput);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot invoke accessor: " + fieldName, e);
        }
    }

    private static Object convertIfNeeded(Object value, Class<?> targetType) {
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Money.class && value instanceof BigDecimal bd) {
            return Money.of(bd);
        }
        return value;
    }

    private static Constructor<?> findConstructor(Class<?> clazz, int paramCount) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == paramCount)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No constructor with " + paramCount + " parameters found in " + clazz.getSimpleName()));
    }
}
