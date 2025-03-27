package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Deprecated
public class EntityGraphPersistence {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JudgeRepository judgeRepository;

    @Transactional
    public <T> List<T> persistAll(List<T> entities) {
        entities.forEach(this::persist);
        return entities;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public <T> T persist(T entity) {
        Set<Object> processedEntities = Collections.synchronizedSet(new HashSet<>());
        try {
            saveEntity(entity, processedEntities);
            return entity;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.AvoidAccessibilityAlteration", "PMD.AvoidDeeplyNestedIfStmts", "PMD.CyclomaticComplexity"})
    private <T> void saveEntity(T entity, Set<Object> processedEntities)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        if (entity == null || processedEntities.contains(entity)) {
            return;
        }

        processedEntities.add(entity);

        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(entity);
                if (fieldValue != null) {
                    if (isEntity(fieldValue)) {
                        saveEntity(fieldValue, processedEntities);
                    } else if (fieldValue instanceof Iterable) {
                        for (Object item : (Iterable<?>) fieldValue) {
                            if (isEntity(item)) {
                                saveEntity(item, processedEntities);
                            }
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        persistOrUpdate(entity);
    }

    private void persistOrUpdate(Object entity) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method getIdMethod = entity.getClass().getMethod("getId");
        Object id = getIdMethod.invoke(entity);
        if (id == null) {
            if (entity instanceof UserAccountEntity user) {
                var systemUser = entityManager.find(UserAccountEntity.class, 0);
                if (user.getLastModifiedById() == null) {
                    user.setLastModifiedById(0);
                }
                if (user.getCreatedBy() == null) {
                    user.setCreatedBy(systemUser);
                }
            }
            if (entity instanceof DefendantEntity def) {
                var courtCase = def.getCourtCase();
                judgeRepository.saveAll(courtCase.getJudges());
                entityManager.persist(courtCase);
            }
            entityManager.persist(entity);
            entityManager.flush();
        } else {
            entityManager.merge(entity);
            entityManager.flush();
        }
    }

    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    private boolean isEntity(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.getClass().isAnnotationPresent(Entity.class);
    }
}
