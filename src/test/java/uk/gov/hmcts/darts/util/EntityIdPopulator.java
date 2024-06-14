package uk.gov.hmcts.darts.util;

import jakarta.persistence.Entity;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Random;
import java.util.function.BiConsumer;

@SuppressWarnings({
    "PMD.AvoidAccessibilityAlteration",
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.AvoidCatchingThrowable",
    "PMD.AvoidThrowingRawExceptionTypes"})
public final class EntityIdPopulator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Random RANDOM = new Random();

    private EntityIdPopulator() {
    }

    public static <T> T withIdsPopulated(T entity) {
        setId(entity, generateId());
        return entity;
    }

    private static Integer generateId() {
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }

    private static <T> void setId(T entity, Integer id) {
        try {
            invokeSetId(entity, id);

            for (Field field : entity.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    if (value.getClass().isAnnotationPresent(Entity.class)) {
                        setNestedEntityId(value);
                    } else if (value instanceof Collection) {
                        setCollectionEntityIds((Collection<?>) value);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }

    private static void setNestedEntityId(Object nestedEntity) throws ReflectiveOperationException {
        Method getIdMethod = nestedEntity.getClass().getMethod("getId");
        Integer nestedId = (Integer) getIdMethod.invoke(nestedEntity);
        if (nestedId == null) {
            setId(nestedEntity, generateId());
        }
    }

    private static void setCollectionEntityIds(Collection<?> collection) throws ReflectiveOperationException {
        for (Object item : collection) {
            if (item.getClass().isAnnotationPresent(Entity.class)) {
                setNestedEntityId(item);
            }
        }
    }

    private static <T> void invokeSetId(T entity, Integer id) {
        try {
            Method setIdMethod = entity.getClass().getMethod("setId", Integer.class);
            MethodHandle setIdHandle = LOOKUP.unreflect(setIdMethod);

            CallSite setterSite = LambdaMetafactory.metafactory(
                LOOKUP,
                "accept",
                MethodType.methodType(BiConsumer.class),
                MethodType.methodType(void.class, Object.class, Object.class),
                setIdHandle,
                MethodType.methodType(void.class, entity.getClass(), Integer.class)
            );

            BiConsumer<T, Integer> setter = (BiConsumer<T, Integer>) setterSite.getTarget().invokeExact();
            setter.accept(entity, id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }
}
