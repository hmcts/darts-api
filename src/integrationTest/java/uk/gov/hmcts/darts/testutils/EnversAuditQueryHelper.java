package uk.gov.hmcts.darts.testutils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import org.hibernate.envers.AuditTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@Component
public class EnversAuditQueryHelper {
    @Autowired
    private EntityManager entityManager;

    public boolean hasBeenAudited(Class<?> entityCls, int entityId) {
        Entity entity = entityCls.getAnnotation(Entity.class);

        if (entity == null) {
            throw new UnsupportedOperationException("Have to specify an entity class");
        }

        AuditTable auditTable = entityCls.getAnnotation(AuditTable.class);

        if (auditTable == null) {
            throw new UnsupportedOperationException("Have to specify an entity class that is auditable");
        }

        Query query = entityManager.createNativeQuery("select COUNT(*) from darts."
                                                          + auditTable.value() + " where " + getPrimaryKey(entityCls) + "=" + entityId);
        return Long.valueOf(entityId).equals(query.getResultList().get(0));
    }

    private String getPrimaryKey(Class entityCls) {

        for (Field field : entityCls.getDeclaredFields()) {
            boolean id = false;
            String columnValue = "";

            for (Annotation annotation : field.getAnnotations()) {
                if (annotation.annotationType().equals(Id.class)) {
                    id = true;
                }

                if (annotation.annotationType().equals(Column.class)) {
                    columnValue = ((Column)annotation).name();
                }
            }

            if (id && !columnValue.isEmpty()) {
                return columnValue;
            }
        }

        throw new UnsupportedOperationException("No primary key found for type " + entityCls);
    }
}