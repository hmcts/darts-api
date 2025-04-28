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

    public static <T> T withIdsPopulatedInt(T entity) {
        setId(entity, generateIdInt());
        return entity;
    }

    public static <T> T withIdsPopulatedLong(T entity) {
        setId(entity, generateIdLong());
        return entity;
    }

    private static Integer generateIdInt() {
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }

    private static Long generateIdLong() {
        return RANDOM.nextLong(Long.MAX_VALUE);
    }

    private static <T> void setId(T entity, Integer id) {
        invokeSetId(entity, id);
        setIdCommon(entity);
    }

    private static <T> void setId(T entity, Long id) {
        invokeSetId(entity, id);
        setIdCommon(entity);
    }

    private static <T> void setIdCommon(T entity) {
        try {

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
        Object nestedId = getIdMethod.invoke(nestedEntity);
        if (nestedId == null) {
            if (getIdMethod.getReturnType().equals(Long.class)) {
                setId(nestedEntity, generateIdLong());
            } else {
                setId(nestedEntity, generateIdInt());
            }
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

    private static <T> void invokeSetId(T entity, Long id) {
        try {
            Method setIdMethod = entity.getClass().getMethod("setId", Long.class);
            MethodHandle setIdHandle = LOOKUP.unreflect(setIdMethod);

            CallSite setterSite = LambdaMetafactory.metafactory(
                LOOKUP,
                "accept",
                MethodType.methodType(BiConsumer.class),
                MethodType.methodType(void.class, Object.class, Object.class),
                setIdHandle,
                MethodType.methodType(void.class, entity.getClass(), Long.class)
            );

            BiConsumer<T, Long> setter = (BiConsumer<T, Long>) setterSite.getTarget().invokeExact();
            setter.accept(entity, id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }
}
