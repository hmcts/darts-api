package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.junit.platform.commons.JUnitException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
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
        return transactionalUtil.executeInTransaction(() -> {
            if (entity instanceof CreatedModifiedBaseEntity createdModifiedBaseEntity) {
                updateCreatedByLastModifiedBy(createdModifiedBaseEntity);
            }
            Method getIdInstanceMethod;
            try {
                getIdInstanceMethod = getIdMethod(entity.getClass());
                Integer id = (Integer) getIdInstanceMethod.invoke(entity);
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


    public void updateCreatedByLastModifiedBy(CreatedModifiedBaseEntity createdModifiedBaseEntity) {
        if (createdModifiedBaseEntity.getCreatedBy() == null) {
            createdModifiedBaseEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
            createdModifiedBaseEntity.setCreatedDateTime(OffsetDateTime.now());
        } else if (createdModifiedBaseEntity.getCreatedBy().getId() == null) {
            UserAccountEntity userAccount = createdModifiedBaseEntity.getCreatedBy();
            updateCreatedByLastModifiedBy(userAccount);
            createdModifiedBaseEntity.setCreatedBy(userAccountRepository.save(userAccount));
        }
        if (createdModifiedBaseEntity.getCreatedDateTime() == null) {
            createdModifiedBaseEntity.setCreatedDateTime(OffsetDateTime.now());
        }

        if (createdModifiedBaseEntity.getLastModifiedBy() == null) {
            createdModifiedBaseEntity.setLastModifiedBy(userAccountRepository.getReferenceById(0));
            createdModifiedBaseEntity.setLastModifiedDateTime(OffsetDateTime.now());
        } else if (createdModifiedBaseEntity.getLastModifiedBy().getId() == null) {
            UserAccountEntity userAccount = createdModifiedBaseEntity.getLastModifiedBy();
            updateCreatedByLastModifiedBy(userAccount);
            createdModifiedBaseEntity.setLastModifiedBy(userAccountRepository.save(userAccount));
        }
        if (createdModifiedBaseEntity.getLastModifiedDateTime() == null) {
            createdModifiedBaseEntity.setLastModifiedDateTime(OffsetDateTime.now());
        }
    }

    @Transactional
    public void saveAll(List<CaseManagementRetentionEntity> caseManagementRetentionsWithEvents) {
        caseManagementRetentionsWithEvents.forEach(this::save);
    }
}
