package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.history.Revisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
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
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

@Service
@AllArgsConstructor
@SuppressWarnings({
    "PMD.ExcessiveImports", "PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity"})
@Getter
@Slf4j
public class DartsDatabaseRetrieval {

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

    private final AnnotationStub annotationStub;
    private final AuditStub auditStub;
    private final CaseDocumentStub caseDocumentStub;
    private final CaseRetentionStub caseRetentionStub;
    private final CourtCaseStub courtCaseStub;
    private final CourthouseStub courthouseStub;
    private final CourtroomStub courtroomStub;
    private final EventStub eventStub;
    private final ExternalObjectDirectoryStub externalObjectDirectoryStub;
    private final HearingStub hearingStub;
    private final MediaStub mediaStub;
    private final MediaRequestStub mediaRequestStub;
    private final TranscriptionStub transcriptionStub;
    private final TranscriptionDocumentStub transcriptionDocumentStub;
    private final TransformedMediaStub transformedMediaStub;
    private final UserAccountStub userAccountStub;

    private final EntityManager entityManager;
    private final CurrentTimeHelper currentTimeHelper;
    private final TransactionalUtil transactionalUtil;
    private final DartsPersistence dartsPersistence;


    public List<EventHandlerEntity> findByHandlerAndActiveTrue(String handlerName) {
        return eventHandlerRepository.findByHandlerAndActiveTrue(handlerName);
    }

    public Optional<CourtCaseEntity> findByCaseByCaseNumberAndCourtHouseName(String someCaseNumber,
                                                                             String someCourthouse) {
        return caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(
            someCaseNumber,
            someCourthouse
        );
    }

    public List<HearingEntity> findByCourthouseCourtroomAndDate(String someCourthouse, String someRoom,
                                                                LocalDate toLocalDate) {
        return hearingRepository.findByCourthouseCourtroomAndDate(someCourthouse, someRoom, toLocalDate);
    }

    public List<EventEntity> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<NotificationEntity> getNotificationsForCase(Integer caseId) {
        return notificationRepository.findByCourtCase_Id(caseId);
    }

    public CourtroomEntity findCourtroomBy(String courthouseName, String courtroomName) {
        return courtroomRepository.findByCourthouseNameAndCourtroomName(courthouseName, courtroomName).orElse(null);
    }

    public CourthouseEntity findCourthouseWithName(String name) {
        return courthouseRepository.findByCourthouseNameIgnoreCase(name).get();
    }

    public ExternalLocationTypeEntity getExternalLocationTypeEntity(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    public ObjectRecordStatusEntity getObjectRecordStatusEntity(ObjectRecordStatusEnum objectRecordStatusEnum) {
        return objectRecordStatusRepository.getReferenceById(objectRecordStatusEnum.getId());
    }

    public List<NotificationEntity> getNotificationFor(String someCaseNumber) {
        return notificationRepository.findAll().stream()
            .filter(notification -> notification.getCourtCase().getCaseNumber().equals(someCaseNumber))
            .toList();
    }

    @Transactional
    public AnnotationEntity findAnnotationById(Integer annotationId) {
        return annotationRepository.findById(annotationId).orElseThrow();
    }

    public AnnotationDocumentEntity findAnnotationDocumentFor(Integer annotationId) {
        return annotationDocumentRepository.findAll().stream()
            .filter(annotationDocument -> annotationDocument.getAnnotation().getId().equals(annotationId))
            .findFirst().orElseThrow(() -> new RuntimeException("No annotation document found for annotation id: " + annotationId));
    }

    public List<ExternalObjectDirectoryEntity> findExternalObjectDirectoryFor(Integer annotationId) {
        var annotationDocumentEntity = annotationDocumentRepository.findAll().stream()
            .filter(annotationDocument -> annotationDocument.getAnnotation().getId().equals(annotationId))
            .findFirst().orElseThrow(() -> new RuntimeException("No annotation document found for annotation id: " + annotationId));

        return externalObjectDirectoryRepository.findAll().stream()
            .filter(externalObjectDirectory -> externalObjectDirectory.getAnnotationDocumentEntity().getId().equals(annotationDocumentEntity.getId()))
            .toList();
    }

    @Transactional
    public List<AnnotationEntity> findAnnotationsFor(Integer hearingId) {
        var hearingEntity = hearingRepository.findById(hearingId).orElseThrow();
        return hearingEntity.getAnnotations().stream().toList();
    }

    @Transactional
    public Integer getLastModifiedByUserId(CreatedModifiedBaseEntity createdModifiedBaseEntity) {
        return createdModifiedBaseEntity.getLastModifiedBy().getId();
    }

    @Transactional
    public EventHandlerEntity findEventHandlerMappingFor(Integer eventHandlerMappingId) {
        return eventHandlerRepository.findById(eventHandlerMappingId).orElseThrow();
    }

    public SecurityGroupEntity getSecurityGroupRef(int id) {
        return securityGroupRepository.getReferenceById(id);
    }


    public CourthouseEntity findCourthouseById(int id) {
        return courthouseRepository.findById(id).orElseThrow();
    }

    public Optional<NodeRegisterEntity> findByNodeId(int id) {
        return nodeRegisterRepository.findById(id);
    }

    public AutomatedTaskEntity getAutomatedTask(int id) {
        return automatedTaskRepository.findById(id).orElseThrow();
    }

    public List<AutomatedTaskEntity> getAllAutomatedTasks() {
        return automatedTaskRepository.findAll();
    }

    public List<AuditEntity> findAudits() {
        return auditRepository.findAll();
    }

    public Revisions<Long, MediaRequestEntity> findMediaRequestRevisionsFor(Integer id) {
        return mediaRequestRepository.findRevisions(id);
    }

    public Revisions<Long, CourthouseEntity> findCourthouseRevisionsFor(Integer id) {
        return courthouseRepository.findRevisions(id);
    }

    public Revisions<Long, UserAccountEntity> findUserAccountRevisionsFor(Integer id) {
        return userAccountRepository.findRevisions(id);
    }

    public Revisions<Long, SecurityGroupEntity> findSecurityGroupRevisionsFor(Integer id) {
        return securityGroupRepository.findRevisions(id);
    }

    public Revisions<Long, TranscriptionEntity> findTranscriptionRevisionsFor(Integer id) {
        return transcriptionRepository.findRevisions(id);
    }

    @Transactional
    public Revisions<Long, TranscriptionWorkflowEntity> findTranscriptionWorkflowRevisionsFor(Integer transcriptionId) {
        var transcription = transcriptionRepository.findById(transcriptionId).orElseThrow();
        var latestWorkflow = transcription.getTranscriptionWorkflowEntities().stream()
            .min(comparing(TranscriptionWorkflowEntity::getWorkflowTimestamp))
            .orElseThrow();

        return transcriptionWorkflowRepository.findRevisions(latestWorkflow.getId());
    }

    @Transactional
    public Revisions<Long, TranscriptionCommentEntity> findTranscriptionCommentRevisionsFor(Integer transcriptionId) {
        var transcription = transcriptionRepository.findById(transcriptionId).orElseThrow();
        var latestComment = transcription.getTranscriptionCommentEntities().stream()
            .min(comparing(TranscriptionCommentEntity::getCreatedDateTime))
            .orElseThrow();

        return transcriptionCommentRepository.findRevisions(latestComment.getId());
    }

    public Revisions<Long, RetentionPolicyTypeEntity> findRetentionPolicyRevisionsFor(Integer id) {
        return retentionPolicyTypeRepository.findRevisions(id);
    }

    @Transactional
    public SecurityRoleEntity findSecurityRole(SecurityRoleEnum role) {
        return securityRoleRepository.findById(role.getId()).orElseThrow();

    }
}