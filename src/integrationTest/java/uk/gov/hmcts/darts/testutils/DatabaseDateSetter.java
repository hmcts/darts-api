package uk.gov.hmcts.darts.testutils;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;

/**
 * A useful test utility that allows us to circumvent the hibernate
 * annotations {@link UpdateTimestamp} and {@link org.hibernate.annotations.CreationTimestamp} and
 * apply our own dates into the database.
 */
@Component
@RequiredArgsConstructor
public class DatabaseDateSetter {
    @Autowired
    private final JdbcTemplate template;

    @Autowired
    private final EntityManager em;

    public <T extends CreatedModifiedBaseEntity>

    @Transactional
        void setLastModifiedDate(T baseEntity, OffsetDateTime timeToSet) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        setLastModifiedDateNoRefresh(baseEntity, timeToSet);
        em.refresh(baseEntity);
    }


    public <T extends CreatedModifiedBaseEntity> void setLastModifiedDateNoRefresh(T baseEntity,
                                                                                   OffsetDateTime timeToSet)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String sql = "UPDATE " + getTable(baseEntity) + " SET last_modified_ts='" + timeToSet.toString()
            + "' where " + getIdColumn(baseEntity) + "=" + getIdValue(baseEntity);

        template.update(sql);
    }

    public <T extends CreatedModifiedBaseEntity>
    @Transactional
        void setCreateDate(T baseEntity, OffsetDateTime timeToSet) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String sql = "UPDATE " + getTable(baseEntity) + " SET last_modified_ts='" + timeToSet.toString()
            + "' where " + getIdColumn(baseEntity) + "=" + getIdValue(baseEntity);

        template.update(sql);
        em.refresh(baseEntity);
    }

    private <T extends CreatedModifiedBaseEntity> String getIdColumn(T entity) {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            Id id = field.getAnnotation(Id.class);
            if (id != null) {
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    return column.name();
                }
            }
        }

        throw new UnsupportedOperationException("No id column found for entity");
    }

    private <T extends CreatedModifiedBaseEntity> String getId(T entity) {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            Id id = field.getAnnotation(Id.class);
            if (id != null) {
                return field.getName();
            }
        }

        throw new UnsupportedOperationException("No id found for entity");
    }

    private <T extends CreatedModifiedBaseEntity> Method getMethodForId(T entity, String id) {
        Method[] methods = entity.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(id)) {
                return method;
            }
        }

        throw new UnsupportedOperationException("No id method found for entity");
    }

    public <T extends CreatedModifiedBaseEntity> Object
    getIdValue(T entity) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String id = getId(entity);
        Method methodId = getMethodForId(entity, "get" + id);
        return methodId.invoke(entity);
    }

    private <T extends CreatedModifiedBaseEntity> String getTable(T entity) {
        Table table = entity.getClass().getAnnotation(Table.class);
        if (table == null) {
            throw new UnsupportedOperationException("No id found for entity");
        }
        return "darts." + table.name();
    }
}