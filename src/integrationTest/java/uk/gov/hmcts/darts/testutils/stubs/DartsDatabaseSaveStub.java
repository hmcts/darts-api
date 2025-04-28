package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.junit.platform.commons.JUnitException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.common.entity.base.LastModifiedBy;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DartsDatabaseSaveStub {

    private final UserAccountRepository userAccountRepository;
    private final EntityManager entityManager;
    private final TransactionalUtil transactionalUtil;


    @Transactional
    public <T> T save(T entity) {
        if (entity == null) {
            return null;
        }
        return transactionalUtil.executeInTransaction(() -> {
            updateCreatedByLastModifiedBy(entity);
            Method getIdInstanceMethod;
            try {
                getIdInstanceMethod = getIdMethod(entity.getClass());
                Object id = getIdInstanceMethod.invoke(entity);
                if (id == null) {
                    this.entityManager.persist(entity);
                    return entity;
                } else {
                    return this.entityManager.merge(entity);
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new JUnitException("Failed to save entity", e);
            }
        });
    }

    private Method getIdMethod(Class<?> clazz) throws NoSuchMethodException {
        String methodName = "getId";
        if (NodeRegisterEntity.class.equals(clazz)) {
            methodName = "getNodeId";
        }
        return clazz.getMethod(methodName);
    }


    public void updateCreatedBy(CreatedBy createdBy) {
        //No need to update values if the entity is a proxy and is not initialized
        if (createdBy instanceof HibernateProxy proxy
            && proxy.getHibernateLazyInitializer().isUninitialized()) {
            return;
        }
        if (createdBy.getCreatedById() == null) {
            createdBy.setCreatedById(0);
            createdBy.setCreatedDateTime(OffsetDateTime.now());
        }
        if (createdBy.getCreatedDateTime() == null) {
            createdBy.setCreatedDateTime(OffsetDateTime.now());
        }
    }

    public void updateLastModifiedBy(LastModifiedBy lastModifiedBy) {
        //No need to update values if the entity is a proxy and is not initialized
        if (lastModifiedBy instanceof HibernateProxy proxy
            && proxy.getHibernateLazyInitializer().isUninitialized()) {
            return;
        }
        if (lastModifiedBy.getLastModifiedById() == null) {
            lastModifiedBy.setLastModifiedById(0);
            lastModifiedBy.setLastModifiedDateTime(OffsetDateTime.now());
        }
        if (lastModifiedBy.getLastModifiedDateTime() == null) {
            lastModifiedBy.setLastModifiedDateTime(OffsetDateTime.now());
        }
    }

    @Transactional
    public void updateCreatedByLastModifiedBy(Object entity) {
        if (entity instanceof CreatedBy createdBy) {
            updateCreatedBy(createdBy);
        }
        if (entity instanceof LastModifiedBy lastModifiedBy) {
            updateLastModifiedBy(lastModifiedBy);
        }
    }

    @Transactional
    public void saveAll(List<CaseManagementRetentionEntity> caseManagementRetentionsWithEvents) {
        caseManagementRetentionsWithEvents.forEach(this::save);
    }
}