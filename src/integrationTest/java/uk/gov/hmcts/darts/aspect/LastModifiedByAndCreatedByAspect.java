package uk.gov.hmcts.darts.aspect;

import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedBaseEntity;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.entity.base.ModifiedBaseEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Aspect
@Component
@Configuration
@RequiredArgsConstructor
@SuppressWarnings("PMD.GodClass")
public class LastModifiedByAndCreatedByAspect {

    @Autowired
    private UserAccountRepository userAccountRepository;

    private static final int SYSTEM_USER_ID = 0;

    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.save(..))")
    public Object handleUserCreatedByAndLastModifiedByForSave(ProceedingJoinPoint joinPoint)
        throws Throwable {

        Object[] args = joinPoint.getArgs();
        Object body = args[0];

        processEntities(body);

        processNested(body);
        return joinPoint.proceed(args);
    }

    @Around("execution(* uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub+.save(..))")
    public Object handleUserCreatedByAndLastModifiedByForSaveDatabaseStub(ProceedingJoinPoint joinPoint)
        throws Throwable {

        Object[] args = joinPoint.getArgs();
        Object body = args[0];

        processEntities(body);

        processNested(body);
        return joinPoint.proceed(args);
    }

    @SuppressWarnings({"PMD.SimplifyBooleanExpressions"})
    @Around("execution(* uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub+.saveAll(..))")
    public Object handleUserCreatedByAndLastModifiedByForSaveAllDatabaseStub(ProceedingJoinPoint joinPoint)
        throws Throwable {

        Object[] objects = joinPoint.getArgs();

        if (objects[0] instanceof List<?> == false) {
            processEntities(objects[0]);
            processNested(objects[0]);
        } else {
            for (Object object : (List) objects[0]) {
                processEntities(object);
                processNested(object);
            }
        }

        return joinPoint.proceed();
    }

    @SuppressWarnings({"PMD.SimplifyBooleanExpressions"})
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.saveAll(..))")
    public Object handleUserCreatedByAndLastModifiedByForSaveAll(ProceedingJoinPoint joinPoint)
        throws Throwable {

        Object[] objects = joinPoint.getArgs();

        if (objects[0] instanceof List<?> == false) {
            processEntities(objects[0]);
            processNested(objects[0]);

        } else {
            for (Object object : (List) objects[0]) {
                processEntities(object);

                processNested(object);
            }
        }

        return joinPoint.proceed();
    }

    @SuppressWarnings({"PMD.SimplifyBooleanExpressions"})
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.saveAndFlush(..))")
    public Object handleUserCreatedByAndLastModifiedByForSaveAndFlush(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] objects = joinPoint.getArgs();

        if (objects[0] instanceof Iterable == false) {
            processEntities(objects[0]);
            processNested(objects[0]);
        } else {
            for (Object object : (Iterable) objects[0]) {
                processEntities(object);
                processNested(object);
            }
        }

        return joinPoint.proceed();
    }


    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.CyclomaticComplexity", "PMD.NcssCount", "PMD.NPathComplexity",
        "PMD.CognitiveComplexity", "PMD.EmptyCatchBlock", "PMD.GodClass", "PMD.AvoidPrintStackTrace",
        "PMD.UselessParentheses", "PMD.SystemPrintln", "PMD.CognitiveComplexity", "PMD.CollapsibleIfStatements"})
    public void processEntities(Object body) {
        if (body instanceof ModifiedBaseEntity) {
            ModifiedBaseEntity entity = (ModifiedBaseEntity) body;

            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            try {
                if (entity.getLastModifiedBy() == null) {
                    entity.setLastModifiedBy(userAccount.get());
                } else if (getId(entity.getLastModifiedBy()) == null) {
                    entity.setLastModifiedBy(userAccount.get());
                } else {
                    entity.setLastModifiedBy(getId(entity.getLastModifiedBy()));
                }
            } catch (LazyInitializationException lazyInitializationException) {
                //do nothing
            }
        }

        if (body instanceof CreatedModifiedBaseEntity) {
            CreatedModifiedBaseEntity entity = (CreatedModifiedBaseEntity) body;
            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            try {
                if (entity.getCreatedBy() == null) {
                    entity.setCreatedBy(userAccount.get());
                } else if (getId(entity.getCreatedBy()) == null) {
                    entity.setCreatedBy(userAccount.get());
                } else {
                    entity.setCreatedBy(getId(entity.getCreatedBy()));
                }

            } catch (LazyInitializationException lazyInitializationException) {
                // do nothing
            }

            try {
                if (entity.getLastModifiedBy() == null) {
                    entity.setLastModifiedBy(userAccount.get());
                } else if (getId(entity.getLastModifiedBy()) == null) {
                    entity.setLastModifiedBy(userAccount.get());
                } else {
                    entity.setLastModifiedBy(getId(entity.getLastModifiedBy()));
                }

            } catch (LazyInitializationException lazyInitializationException) {
                // do nothing
            }

        }

        if (body instanceof MandatoryCreatedBaseEntity) {
            MandatoryCreatedBaseEntity entity = (MandatoryCreatedBaseEntity) body;
            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            try {
                if (entity.getCreatedBy() == null) {
                    entity.setCreatedBy(userAccount.get());
                } else if (getId(entity.getCreatedBy()) == null) {
                    entity.setCreatedBy(userAccount.get());
                } else {
                    entity.setCreatedBy(getId(entity.getCreatedBy()));
                }


            } catch (LazyInitializationException lazyInitializationException) {
                // do nothing

            }
        }

        if (body instanceof MandatoryCreatedModifiedBaseEntity) {
            MandatoryCreatedModifiedBaseEntity entity = (MandatoryCreatedModifiedBaseEntity) body;
            Optional<UserAccountEntity> userAccount = userAccountRepository.findById(SYSTEM_USER_ID);

            try {
                if (entity.getCreatedBy() == null) {
                    entity.setCreatedBy(userAccount.get());
                } else if (getId(entity.getCreatedBy()) == null) {
                    entity.setCreatedBy(userAccount.get());
                } else {
                    entity.setCreatedBy(getId(entity.getCreatedBy()));
                }

            } catch (LazyInitializationException lazyInitializationException) {
                // do nothing

            }

            try {
                if (entity.getLastModifiedBy() == null) {
                    entity.setLastModifiedBy(userAccount.get());
                } else if (getId(entity.getLastModifiedBy()) == null) {
                    entity.setLastModifiedBy(userAccount.get());
                } else {
                    entity.setLastModifiedBy(getId(entity.getLastModifiedBy()));
                }
            } catch (LazyInitializationException lazyInitializationException) {
                // do nothing
            }
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private UserAccountEntity getId(Object id) {


        for (Field method : UserAccountEntity.class.getDeclaredFields()) {
            Id ann = method.getAnnotation(Id.class);
            if (ann != null) {
                try {
                    Object fndId = id.getClass().getMethod("get" + StringUtils.capitalize(method.getName())).invoke(id);

                    if (fndId != null) {
                        Optional<UserAccountEntity> fndObject = userAccountRepository.findById(Integer.valueOf(fndId.toString()));
                        if (fndObject.isPresent() && fndObject.get().getLastModifiedBy() != null) {
                            return fndObject.get();
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    // dont do anything
                }
            }
        }


        return null;
    }

    @SuppressWarnings("PMD.AvoidPrintStackTrace")
    private void processNested(Object object) {
        try {
            List<Object> objectsLst = new ArrayList<>();
            processNested(object, objectsLst);
            for (Object myobject : objectsLst) {
                processEntities(myobject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"PMD.GodClass", "PMD.AvoidPrintStackTrace",
        "PMD.UselessParentheses", "PMD.SystemPrintln",
        "PMD.CognitiveComplexity",
        "PMD.SignatureDeclareThrowsException", "PMD.CyclomaticComplexity",
        "PMD.CollapsibleIfStatements", "PMD.AvoidAccessibilityAlteration"})
    public static void processNested(Object object, List<Object> objects) throws Exception {

        if (object != null) {
            for (Field field : object.getClass().getDeclaredFields()) {
                if ((field.getType().getComponentType() != null
                    && !field.getType().getComponentType().isPrimitive()
                    && !field.getType().getCanonicalName().equals(String.class.getCanonicalName())
                    ||
                    (!field.getType().isPrimitive()
                        && !field.getType().getCanonicalName().equals(String.class.getCanonicalName())))) {
                    field.setAccessible(true);
                    Object fieldObj = field.get(object);

                    if (fieldObj != null && !(fieldObj instanceof Collection) && !objects.contains(fieldObj)
                        && !fieldObj.getClass().getPackageName().startsWith("java.")) {
                        objects.add(fieldObj);
                        processNested(fieldObj, objects);
                    }
                    if (fieldObj instanceof Collection) {
                        for (Object colObject : (Collection) fieldObj) {
                            if (!objects.contains(colObject)) {
                                if (fieldObj != null) {
                                    objects.add(colObject);
                                    processNested(colObject, objects);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({"PMD.GodClass", "PMD.AvoidPrintStackTrace",
        "PMD.UselessParentheses", "PMD.SystemPrintln", "PMD.SignatureDeclareThrowsException"})
    public static void main(String[] args) throws Exception {
        CourtCaseEntity entity = new CourtCaseEntity();
        CourthouseEntity courthouse = new CourthouseEntity();
        entity.setCourthouse(courthouse);
        RegionEntity regionEntity = new RegionEntity();
        courthouse.setRegion(regionEntity);
        List<Object> list = new ArrayList<>();
        processNested(entity, list);
        System.out.println("OUTCOME " + list);
    }
}