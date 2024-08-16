package uk.gov.hmcts.darts.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedBaseEntity;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.entity.base.ModifiedBaseEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Optional;

@Aspect
@Component
@Configuration
@RequiredArgsConstructor
public class LastModifiedByAndCreatedByAspect {

    @Autowired
    private UserAccountRepository userAccountRepository;

    private static final int SYSTEM_USER_ID = 0;

    @Around("execution(* jakarta.persistence.EntityManager+.persist(..))")
    public Object handleUserCreatedByAndLastModifiedByForPersist(ProceedingJoinPoint joinPoint)
        throws Throwable {

        Object[] args = joinPoint.getArgs();
        Object body = args[0];

        processEntities(body);

        return joinPoint.proceed();
    }

    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.save(..))")
    public Object handleUserCreatedByAndLastModifiedByForSave(ProceedingJoinPoint joinPoint)
        throws Throwable {

        Object[] args = joinPoint.getArgs();
        Object body = args[0];

        processEntities(body);

        return joinPoint.proceed();
    }

    @SuppressWarnings({"PMD.SimplifyBooleanExpressions"})
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.saveAndFlush(..))")
    public Object handleUserCreatedByAndLastModifiedByForSaveAndFlush(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] objects = joinPoint.getArgs();

        if (objects[0] instanceof Iterable == false) {
            processEntities(objects[0]);
        } else {
            for (Object object : (Iterable)objects[0]) {
                processEntities(object);
            }
        }

        return joinPoint.proceed();
    }


    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    private void processEntities(Object body) {
        if (body instanceof ModifiedBaseEntity) {
            ModifiedBaseEntity entity = (ModifiedBaseEntity) body;

            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            UserAccountEntity existingModifiedBy = entity.getLastModifiedBy();
            try {
                if (existingModifiedBy == null) {
                    entity.setLastModifiedBy(userAccount.get());
                } else {
                    userAccountRepository.save(existingModifiedBy);
                }
            }
            catch (LazyInitializationException lazyInitializationException) {

            }
        }

        if (body instanceof CreatedModifiedBaseEntity) {
            CreatedModifiedBaseEntity entity = (CreatedModifiedBaseEntity) body;
            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            try {
                if (entity.getLastModifiedBy() == null) {
                    entity.setLastModifiedBy(userAccount.get());
                }
            }
            catch (LazyInitializationException lazyInitializationException) {

            }

            try {
                if (entity.getCreatedBy() == null) {
                    entity.setCreatedBy(userAccount.get());
                }
            } catch (LazyInitializationException lazyInitializationException) {

            }
        }

        if (body instanceof MandatoryCreatedBaseEntity) {
            MandatoryCreatedBaseEntity entity = (MandatoryCreatedBaseEntity) body;
            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            try {
                if (entity.getCreatedBy() == null) {
                    entity.setCreatedBy(userAccount.get());
                }
            } catch (LazyInitializationException lazyInitializationException) {

            }
        }

        if (body instanceof MandatoryCreatedModifiedBaseEntity) {
            MandatoryCreatedModifiedBaseEntity entity = (MandatoryCreatedModifiedBaseEntity) body;
            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            try {
                if (entity.getLastModifiedBy() == null) {
                    entity.setLastModifiedBy(userAccount.get());
                }
            }
            catch (LazyInitializationException lazyInitializationException) {

            }

            try {
                if (entity.getCreatedBy() == null) {
                    entity.setCreatedBy(userAccount.get());
                }
            } catch (LazyInitializationException lazyInitializationException) {

            }

        }
    }

}