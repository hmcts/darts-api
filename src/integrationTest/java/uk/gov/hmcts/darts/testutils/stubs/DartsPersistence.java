package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
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
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@SuppressWarnings({
    "PMD.ExcessiveImports", "PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity"})
@Getter
@Slf4j
public class DartsPersistence {

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

    private final EntityManager entityManager;
    private final CurrentTimeHelper currentTimeHelper;
    private final TransactionalUtil transactionalUtil;

    @Transactional
    public AnnotationEntity save(AnnotationEntity annotationEntity) {
        save(annotationEntity.getCurrentOwner());
        saveHearingList(annotationEntity.getHearingList());
        return annotationRepository.save(annotationEntity);
    }

    @Transactional
    public HearingEntity save(HearingEntity hearingEntity) {
        saveMediaList(hearingEntity.getMediaList());
        save(hearingEntity.getCourtroom().getCourthouse());
        save(hearingEntity.getCourtroom());
        save(hearingEntity.getCreatedBy());
        save(hearingEntity.getLastModifiedBy());
        if (hearingEntity.getCourtCase().getId() == null) {
            save(hearingEntity.getCourtCase());
        } else {
            entityManager.merge(hearingEntity.getCourtCase());
        }
        return hearingRepository.save(hearingEntity);
    }

    @Transactional
    public CourthouseEntity save(CourthouseEntity courthouse) {
        save(courthouse.getCreatedBy());
        return courthouseRepository.save(courthouse);
    }

    @Transactional
    public CourtroomEntity save(CourtroomEntity courtroom) {
        if (isNull(courtroom.getCourthouse().getId())) {
            save(courtroom.getCourthouse());
        }
        save(courtroom.getCreatedBy());
        return courtroomRepository.save(courtroom);
    }

    @Transactional
    public MediaRequestEntity save(MediaRequestEntity mediaRequestEntity) {
        save(mediaRequestEntity.getHearing());
        save(mediaRequestEntity.getRequestor());
        save(mediaRequestEntity.getCurrentOwner());
        save(mediaRequestEntity.getCreatedBy());
        save(mediaRequestEntity.getLastModifiedBy());
        return mediaRequestRepository.save(mediaRequestEntity);
    }

    @Transactional
    public CourtCaseEntity save(CourtCaseEntity courtCase) {
        save(courtCase.getCreatedBy());
        save(courtCase.getLastModifiedBy());
        judgeRepository.saveAll(courtCase.getJudges());
        save(courtCase.getCourthouse());
        courtCase.getProsecutorList().forEach(this::save);
        courtCase.getDefenceList().forEach(this::save);
        courtCase.getDefendantList().forEach(this::save);
        saveHearingList(courtCase.getHearings());
        caseRepository.save(courtCase);
        return courtCase;
    }

    @Transactional
    public ExternalObjectDirectoryEntity save(ExternalObjectDirectoryEntity eod) {
        var locationType = externalLocationTypeRepository.getReferenceById(eod.getExternalLocationType().getId());
        eod.setExternalLocationType(locationType);
        var recordStatus = objectRecordStatusRepository.getReferenceById(eod.getStatus().getId());
        eod.setStatus(recordStatus);

        save(eod.getMedia());
        save(eod.getCreatedBy());
        save(eod.getLastModifiedBy());
        return externalObjectDirectoryRepository.save(eod);
    }

    @Transactional
    public DefenceEntity save(DefenceEntity defenceEntity) {
        save(defenceEntity.getCreatedBy());
        save(defenceEntity.getLastModifiedBy());
        return defenceEntity;
    }

    @Transactional
    public DefendantEntity save(DefendantEntity defendantEntity) {
        save(defendantEntity.getCreatedBy());
        save(defendantEntity.getLastModifiedBy());
        return defendantEntity;
    }

    @Transactional
    public ProsecutorEntity save(ProsecutorEntity prosecutor) {
        save(prosecutor.getCreatedBy());
        save(prosecutor.getLastModifiedBy());
        return prosecutor;
    }

    @Transactional
    public TranscriptionEntity save(TranscriptionEntity transcriptionEntity) {
        save(transcriptionEntity.getHearing());
        save(transcriptionEntity.getCourtroom());
        save(transcriptionEntity.getCourtCase());
        save(transcriptionEntity.getCreatedBy());
        save(transcriptionEntity.getLastModifiedBy());
        var transcription = transcriptionRepository.save(transcriptionEntity);
        save(transcription.getCreatedBy());
        transcription.getTranscriptionDocumentEntities().forEach(td -> {
            save(td.getUploadedBy());
            save(td.getLastModifiedBy());
            transcriptionDocumentRepository.save(td);
        });
        return transcription;
    }

    @Transactional
    public EventEntity save(EventEntity eventEntity) {
        save(eventEntity.getCourtroom());
        save(eventEntity.getCreatedBy());
        save(eventEntity.getLastModifiedBy());
        return eventRepository.save(eventEntity);
    }

    @Transactional
    public RetentionPolicyTypeEntity save(RetentionPolicyTypeEntity retentionPolicyTypeEntity) {
        save(retentionPolicyTypeEntity.getLastModifiedBy());
        save(retentionPolicyTypeEntity.getCreatedBy());
        return retentionPolicyTypeRepository.save(retentionPolicyTypeEntity);
    }

    @Transactional
    public CaseManagementRetentionEntity save(CaseManagementRetentionEntity caseManagementRetentionEntity) {
        save(caseManagementRetentionEntity.getCourtCase());
        save(caseManagementRetentionEntity.getEventEntity());
        save(caseManagementRetentionEntity.getRetentionPolicyTypeEntity());
        return caseManagementRetentionRepository.saveAndFlush(caseManagementRetentionEntity);
    }

    @Transactional
    public UserAccountEntity save(UserAccountEntity userAccountEntity) {
        var systemUser = userAccountRepository.getReferenceById(0);
        userAccountEntity.setCreatedBy(systemUser);
        userAccountEntity.setLastModifiedBy(systemUser);

        return userAccountRepository.save(userAccountEntity);
    }

    @Transactional
    public AnnotationDocumentEntity save(AnnotationDocumentEntity annotationDocumentEntity) {
        if (annotationDocumentEntity.getAnnotation().getId() == null) {
            save(annotationDocumentEntity.getAnnotation());
        }
        save(annotationDocumentEntity.getLastModifiedBy());
        return annotationDocumentRepository.save(annotationDocumentEntity);

    }

    @Transactional
    public MediaEntity save(MediaEntity media) {
        save(media.getCourtroom());
        save(media.getCreatedBy());
        save(media.getLastModifiedBy());
        return mediaRepository.save(media);
    }

    @Transactional
    public TransformedMediaEntity save(TransformedMediaEntity transformedMedia) {
        save(transformedMedia.getCreatedBy());
        save(transformedMedia.getLastModifiedBy());
        return transformedMediaRepository.save(transformedMedia);
    }

    @Transactional
    public void saveAll(UserAccountEntity... testUsers) {
        stream(testUsers).forEach(user -> {
            UserAccountEntity systemUser = userAccountRepository.getReferenceById(0);
            user.setCreatedBy(systemUser);
            user.setLastModifiedBy(systemUser);
        });
        userAccountRepository.saveAll(asList(testUsers));
    }

    @Transactional
    public void saveAll(HearingEntity... hearingEntities) {
        stream(hearingEntities).forEach(this::save);
    }

    @Transactional
    public void saveAll(List<HearingEntity> hearingEntities) {
        for (HearingEntity hearingEntity : hearingEntities) {
            save(hearingEntity);
        }
    }

    public List<DailyListEntity> saveAll(DailyListEntity... dailyListEntity) {
        return dailyListRepository.saveAll(asList(dailyListEntity));
    }

    public void saveAll(AnnotationDocumentEntity... annotationDocuments) {
        stream(annotationDocuments).forEach(this::save);
    }

    public void saveMediaList(List<MediaEntity> mediaList) {
        mediaList.forEach(this::save);
    }

    @Transactional
    public void saveHearingList(List<HearingEntity> hearingEntities) {
        for (HearingEntity hearingEntity : hearingEntities) {
            save(hearingEntity);
        }
    }
}