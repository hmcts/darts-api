package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
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
    public HearingEntity save(HearingEntity hearing) {
        return save(hearing, true);
    }

    @Transactional
    public HearingEntity save(HearingEntity hearing, boolean processMedia) {
        if (hearing.getId() == null) {
            save(hearing.getCourtroom().getCourthouse());
            save(hearing.getCourtroom());
            save(hearing.getCreatedBy());
            save(hearing.getLastModifiedBy());
            saveJudgeList(hearing.getJudges());
            if (hearing.getCourtCase().getId() == null) {
                save(hearing.getCourtCase());
            } else {
                entityManager.merge(hearing.getCourtCase());
            }

            if (processMedia) {
                saveMediaList(hearing.getMediaList(), false);
            }
            hearingRepository.save(hearing);
        }
        return hearing;
    }

    @Transactional
    public CourthouseEntity save(CourthouseEntity courthouse) {

        if (courthouse.getId() == null) {
            save(courthouse.getCreatedBy());
            save(courthouse.getLastModifiedBy());
            courthouseRepository.save(courthouse);
        }

        return courthouse;
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
    public MediaRequestEntity save(MediaRequestEntity mediaRequest) {
        save(mediaRequest.getHearing());
        save(mediaRequest.getRequestor());
        save(mediaRequest.getCurrentOwner());
        save(mediaRequest.getCreatedBy());
        save(mediaRequest.getLastModifiedBy());

        return mediaRequestRepository.save(mediaRequest);
    }

    @Transactional
    public MediaLinkedCaseEntity save(MediaLinkedCaseEntity mediaRequest) {
        save(mediaRequest.getCourtCase());
        return mediaLinkedCaseRepository.save(mediaRequest);
    }

    @Transactional
    public CourtCaseEntity save(CourtCaseEntity courtCase) {
        save(courtCase.getCreatedBy());
        save(courtCase.getLastModifiedBy());
        saveJudgeList(courtCase.getJudges());
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

        if (eod.getMedia() != null) {
            save(eod.getMedia());
        }

        save(eod.getCreatedBy());
        save(eod.getLastModifiedBy());
        return externalObjectDirectoryRepository.save(eod);
    }

    @Transactional
    public DefenceEntity save(DefenceEntity defence) {
        save(defence.getCreatedBy());
        save(defence.getLastModifiedBy());
        return defence;
    }

    @Transactional
    public DefendantEntity save(DefendantEntity defendant) {
        save(defendant.getCreatedBy());
        save(defendant.getLastModifiedBy());
        return defendant;
    }

    @Transactional
    public ProsecutorEntity save(ProsecutorEntity prosecutor) {
        save(prosecutor.getCreatedBy());
        save(prosecutor.getLastModifiedBy());
        return prosecutor;
    }

    @Transactional
    public TranscriptionEntity save(TranscriptionEntity transcription) {
        save(transcription.getHearing());
        save(transcription.getCourtroom());
        save(transcription.getCourtCase());
        save(transcription.getCreatedBy());
        save(transcription.getLastModifiedBy());
        transcriptionRepository.save(transcription);
        save(transcription.getCreatedBy());
        transcription.getTranscriptionDocumentEntities().forEach(td -> {
            save(td.getUploadedBy());
            save(td.getLastModifiedBy());
            transcriptionDocumentRepository.save(td);
        });
        return transcription;
    }

    @Transactional
    public EventEntity save(EventEntity event) {
        save(event.getCourtroom());
        save(event.getCreatedBy());
        save(event.getLastModifiedBy());
        return eventRepository.save(event);
    }

    @Transactional
    public RetentionPolicyTypeEntity save(RetentionPolicyTypeEntity retentionPolicyType) {
        save(retentionPolicyType.getLastModifiedBy());
        save(retentionPolicyType.getCreatedBy());
        return retentionPolicyTypeRepository.save(retentionPolicyType);
    }

    @Transactional
    public CaseManagementRetentionEntity save(CaseManagementRetentionEntity caseManagementRetention) {
        save(caseManagementRetention.getCourtCase());
        save(caseManagementRetention.getEventEntity());
        save(caseManagementRetention.getRetentionPolicyTypeEntity());
        return caseManagementRetentionRepository.saveAndFlush(caseManagementRetention);
    }

    @Transactional
    public UserAccountEntity save(UserAccountEntity userAccount) {
        var systemUser = userAccountRepository.getReferenceById(0);
        userAccount.setCreatedBy(systemUser);
        userAccount.setLastModifiedBy(systemUser);

        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public AnnotationDocumentEntity save(AnnotationDocumentEntity annotationDocument) {
        if (annotationDocument.getAnnotation().getId() == null) {
            save(annotationDocument.getAnnotation());
        }
        save(annotationDocument.getLastModifiedBy());
        return annotationDocumentRepository.save(annotationDocument);
    }

    @Transactional
    public void save(JudgeEntity judge) {
        save(judge.getCreatedBy());
        save(judge.getLastModifiedBy());
        judgeRepository.save(judge);
    }

    public MediaEntity save(MediaEntity media) {
        return save(media, true);
    }

    public CaseDocumentEntity save(CaseDocumentEntity caseDocumentEntity) {
        save(caseDocumentEntity.getCourtCase());
        return caseDocumentRepository.save(caseDocumentEntity);
    }

    @Transactional
    public MediaEntity save(MediaEntity media, boolean processhHearing) {

        if (media.getId() == null) {
            save(media.getCourtroom());
            save(media.getCreatedBy());
            save(media.getLastModifiedBy());

            if (media.getMediaLinkedCaseList() != null) {
                saveLinkedCaseList(media.getMediaLinkedCaseList());
            }

            mediaRepository.save(media);
            if (media.getHearingList() != null && processhHearing) {
                for (HearingEntity hearingEntity : media.getHearingList()) {
                    hearingEntity.getMediaList().add(media);
                    save(hearingEntity, false);
                }
            }
        }
        return media;
    }

    @Transactional
    public TransformedMediaEntity save(TransformedMediaEntity transformedMedia) {
        save(transformedMedia.getCreatedBy());
        save(transformedMedia.getLastModifiedBy());
        save(transformedMedia.getMediaRequest());
        return transformedMediaRepository.save(transformedMedia);
    }

    @Transactional
    public void saveAll(UserAccountEntity... userAccounts) {
        stream(userAccounts).forEach(user -> {
            UserAccountEntity systemUser = userAccountRepository.getReferenceById(0);
            user.setCreatedBy(systemUser);
            user.setLastModifiedBy(systemUser);
        });
        userAccountRepository.saveAll(asList(userAccounts));
    }

    @Transactional
    public void saveAll(HearingEntity... hearings) {
        stream(hearings).forEach(this::save);
    }

    @Transactional
    public void saveAll(AnnotationDocumentEntity... annotationDocuments) {
        stream(annotationDocuments).forEach(this::save);
    }

    private void saveMediaList(List<MediaEntity> mediaList, boolean processHearings) {
        mediaList.forEach(media -> {
            if (media.getId() == null) {
                save(media, processHearings);
            }
        });
    }

    private void saveHearingList(List<HearingEntity> hearings) {
        hearings.forEach(hearing -> {
            if (hearing.getId() == null) {
                save(hearing);
            }
        });
    }

    private void saveLinkedCaseList(List<MediaLinkedCaseEntity> linkedCases) {
        linkedCases.forEach(cse -> {
            if (cse.getId() == null) {
                save(cse);
            }
        });
    }

    private void saveJudgeList(List<JudgeEntity> judges) {
        judges.forEach(this::save);
    }

}