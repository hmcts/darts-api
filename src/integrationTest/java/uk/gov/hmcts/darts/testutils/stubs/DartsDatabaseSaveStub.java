package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.junit.platform.commons.JUnitException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Service
@AllArgsConstructor
public class DartsDatabaseSaveStub {

    private final UserAccountRepository userAccountRepository;
    private final EntityManager entityManager;


    @Transactional
    public <T> T save(T entity) {
        if (entity instanceof CreatedModifiedBaseEntity createdModifiedBaseEntity) {
            updateCreatedByLastModifiedBy(createdModifiedBaseEntity);
        }
        Method getIdInstanceMethod;
        try {
            getIdInstanceMethod = entity.getClass().getMethod("getId");
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
    }

    private void updateCreatedByLastModifiedBy(CreatedModifiedBaseEntity createdModifiedBaseEntity) {
        if (createdModifiedBaseEntity.getCreatedBy() == null) {
            createdModifiedBaseEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
        } else if (createdModifiedBaseEntity.getCreatedBy().getId() == null) {
            UserAccountEntity userAccount = createdModifiedBaseEntity.getCreatedBy();
            updateCreatedByLastModifiedBy(userAccount);
            createdModifiedBaseEntity.setCreatedBy(userAccountRepository.save(userAccount));
        }

        if (createdModifiedBaseEntity.getLastModifiedBy() == null) {
            createdModifiedBaseEntity.setLastModifiedBy(userAccountRepository.getReferenceById(0));
        } else if (createdModifiedBaseEntity.getLastModifiedBy().getId() == null) {
            UserAccountEntity userAccount = createdModifiedBaseEntity.getLastModifiedBy();
            updateCreatedByLastModifiedBy(userAccount);
            createdModifiedBaseEntity.setLastModifiedBy(userAccountRepository.save(userAccount));
        }
    }
}
