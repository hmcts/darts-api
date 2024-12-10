package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.junit.platform.commons.JUnitException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        if (entity == null
            || (entity instanceof HibernateProxy proxy
            && proxy.getHibernateLazyInitializer().isUninitialized())) {
            return entity;
        }
        return transactionalUtil.executeInTransaction(() -> {
            Authentication authentication = null;
            //Remove the authentication from the context to bypass UserAuditListener.
            //This will be added back again at the end of the method.
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                authentication = SecurityContextHolder.getContext().getAuthentication();
                SecurityContextHolder.getContext().setAuthentication(null);
            }

            if (entity instanceof CreatedModifiedBaseEntity createdModifiedBaseEntity) {
                updateCreatedByLastModifiedBy(createdModifiedBaseEntity);
            }
            Method getIdInstanceMethod;
            try {
                this.entityManager.clear();
                getIdInstanceMethod = getIdMethod(entity.getClass());
                Integer id = (Integer) getIdInstanceMethod.invoke(entity);
                T toReturn;
                if (id == null) {
                    this.entityManager.persist(entity);
                    this.entityManager.flush();
                    toReturn = entity;
                } else {
                    toReturn = this.entityManager.merge(entity);
                    this.entityManager.flush();
                }
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                return toReturn;
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


    @Transactional
    public void updateCreatedByLastModifiedBy(CreatedModifiedBaseEntity createdModifiedBaseEntity) {
        //No need to update values if the entity is a proxy and is not initialized
        if (createdModifiedBaseEntity instanceof HibernateProxy proxy
            && proxy.getHibernateLazyInitializer().isUninitialized()) {
            return;
        }
        if (createdModifiedBaseEntity.getCreatedBy() == null) {
            createdModifiedBaseEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
            createdModifiedBaseEntity.setCreatedDateTime(OffsetDateTime.now());
        } else if (createdModifiedBaseEntity.getCreatedBy().getId() == null) {
            UserAccountEntity userAccount = createdModifiedBaseEntity.getCreatedBy();
            updateCreatedByLastModifiedBy(userAccount);
            createdModifiedBaseEntity.setCreatedBy(userAccountRepository.save(userAccount));
        } else {
            userAccountRepository.save(createdModifiedBaseEntity.getCreatedBy());
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
        } else {
            userAccountRepository.save(createdModifiedBaseEntity.getLastModifiedBy());
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
