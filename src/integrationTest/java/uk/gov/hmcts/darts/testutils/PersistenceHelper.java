package uk.gov.hmcts.darts.testutils;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.TransientPropertyValueException;
import org.junit.platform.commons.JUnitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.common.repository.DefenceRepository;
import uk.gov.hmcts.darts.common.repository.DefendantRepository;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.common.repository.NotificationRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.ProsecutorRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PersistenceHelper {

    private final EntityManager entityManager;

    private final EntityManagerFactory entityManagerFactory;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final AnnotationRepository annotationRepository;
    private final AuditRepository auditRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseManagementRetentionRepository caseManagementRetentionRepository;
    private final CaseRepository caseRepository;
    private final CaseRetentionRepository caseRetentionRepository;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;
    private final DailyListRepository dailyListRepository;
    private final DefenceRepository defenceRepository;
    private final DefendantRepository defendantRepository;
    private final EventHandlerRepository eventHandlerRepository;
    private final EventRepository eventRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;
    private final HearingRepository hearingRepository;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;
    private final JudgeRepository judgeRepository;
    private final MediaRepository mediaRepository;
    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final NodeRegisterRepository nodeRegisterRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ProsecutorRepository prosecutorRepository;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final TranscriptionCommentRepository transcriptionCommentRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final RegionRepository regionRepository;
    private final AutomatedTaskRepository automatedTaskRepository;
    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final EventLinkedCaseRepository eventLinkedCaseRepository;

    @Transactional
    public CourtCaseEntity save(CourtCaseEntity courtCase) {

        save(courtCase.getCourthouse());
        save(courtCase.getCreatedBy());
        save(courtCase.getLastModifiedBy());
//        return saveWithTransientEntities(courtCase);
        return caseRepository.save(courtCase);
    }

    @Transactional
    public CourthouseEntity save(CourthouseEntity courthouse) {
        userAccountRepository.save(courthouse.getCreatedBy());
        return courthouseRepository.save(courthouse);
    }

    @Transactional
    public <T> T save(T entity) {
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

    @Transactional
    public <T> T saveWithTransientEntities(T entity) {
        try {
            // Check and persist transient entities
            Class<?> clazz = entity.getClass();
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(entity);
                    if (fieldValue != null && isEntity(fieldValue)) {
                        // Check if the entity is transient
                        Method getIdMethod = fieldValue.getClass().getMethod("getId");
                        Object id = getIdMethod.invoke(fieldValue);
                        if (id == null || (id instanceof Integer && (Integer) id == 0)) {
                            // Save the transient entity
                            entityManager.persist(fieldValue);
                            entityManager.flush();
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }

            // Proceed with original save logic
            Method getIdInstanceMethod = entity.getClass().getMethod("getId");
            Integer id = (Integer) getIdInstanceMethod.invoke(entity);
            if (id == null) {
                entityManager.persist(entity);
                return entity;
            } else {
                return entityManager.merge(entity);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new JUnitException("Failed to save entity", e);
        }
    }

    private boolean isEntity(Object obj) {
        return obj.getClass().isAnnotationPresent(Entity.class);
    }

}
