package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.common.entity.base.LastModifiedBy;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;
import uk.gov.hmcts.darts.common.repository.ArmRpoStateRepository;
import uk.gov.hmcts.darts.common.repository.ArmRpoStatusRepository;
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
import uk.gov.hmcts.darts.common.repository.RetentionConfidenceCategoryMapperRepository;
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
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.test.common.data.builder.DbInsertable;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.util.ArrayList;
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
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;
    private final ArmRpoStateRepository armRpoStateRepository;
    private final ArmRpoStatusRepository armRpoStatusRepository;
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
    private final RetentionConfidenceCategoryMapperRepository retentionConfidenceCategoryMapperRepository;

    private final EntityManager entityManager;
    private final CurrentTimeHelper currentTimeHelper;
    private final TransactionalUtil transactionalUtil;

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public RetentionConfidenceCategoryMapperEntity save(RetentionConfidenceCategoryMapperEntity retentionConfidenceCategoryMapperEntity) {
        retentionConfidenceCategoryMapperEntity = (RetentionConfidenceCategoryMapperEntity) preCheckPersist(retentionConfidenceCategoryMapperEntity);

        if (retentionConfidenceCategoryMapperEntity.getId() == null) {
            retentionConfidenceCategoryMapperEntity.setLastModifiedBy(save(retentionConfidenceCategoryMapperEntity.getLastModifiedBy()));
            retentionConfidenceCategoryMapperEntity.setCreatedBy(save(retentionConfidenceCategoryMapperEntity.getCreatedBy()));

            retentionConfidenceCategoryMapperEntity = retentionConfidenceCategoryMapperRepository.save(retentionConfidenceCategoryMapperEntity);
        } else {
            retentionConfidenceCategoryMapperEntity = entityManager.merge(retentionConfidenceCategoryMapperEntity);
        }

        return retentionConfidenceCategoryMapperEntity;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public HearingEntity save(HearingEntity hearing) {
        hearing = (HearingEntity) preCheckPersist(hearing);

        hearing.setCourtCase(save(hearing.getCourtCase()));
        hearing.setCourtroom(save(hearing.getCourtroom()));
        hearing.setCreatedBy(save(hearing.getCreatedBy()));
        hearing.setLastModifiedBy(save(hearing.getLastModifiedBy()));
        hearing.setJudges(saveJudgeList(hearing.getJudges()));
        hearing = hearingRepository.save(hearing);

        saveMediaList(hearing.getMediaList());
        return hearing;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public AnnotationEntity save(AnnotationEntity annotationEntity) {
        annotationEntity = (AnnotationEntity) preCheckPersist(annotationEntity);

        if (annotationEntity.getId() == null) {
            annotationEntity.setCurrentOwner(save(annotationEntity.getCurrentOwner()));
            annotationEntity.setCreatedBy(save(annotationEntity.getCreatedBy()));
            annotationEntity.setLastModifiedBy(save(annotationEntity.getLastModifiedBy()));
            if (annotationEntity.getHearingList() != null) {
                annotationEntity.setHearingList(saveHearingEntity(annotationEntity.getHearingList()));
            }
            return annotationRepository.save(annotationEntity);
        } else {
            return entityManager.merge(annotationEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public CourthouseEntity save(CourthouseEntity courthouse) {
        courthouse = (CourthouseEntity) preCheckPersist(courthouse);

        if (courthouse.getId() == null) {
            courthouse.setCreatedBy(save(courthouse.getCreatedBy()));
            courthouse.setLastModifiedBy(save(courthouse.getLastModifiedBy()));
            courthouseRepository.save(courthouse);
        } else {
            courthouse = entityManager.merge(courthouse);
        }

        return courthouse;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public CourtroomEntity save(CourtroomEntity courtroom) {
        courtroom = (CourtroomEntity) preCheckPersist(courtroom);

        if (courtroom.getId() == null) {
            if (isNull(courtroom.getCourthouse().getId())) {
                courtroom.setCourthouse(save(courtroom.getCourthouse()));
            }
            courtroom.setCreatedBy(save(courtroom.getCreatedBy()));
            return courtroomRepository.save(courtroom);
        } else {
            return entityManager.merge(courtroom);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public MediaRequestEntity save(MediaRequestEntity mediaRequest) {
        mediaRequest = (MediaRequestEntity) preCheckPersist(mediaRequest);

        if (mediaRequest.getId() == null) {
            mediaRequest.setHearing(save(mediaRequest.getHearing()));
            mediaRequest.setRequestor(save(mediaRequest.getRequestor()));
            mediaRequest.setCurrentOwner(save(mediaRequest.getCurrentOwner()));
            mediaRequest.setCreatedBy(save(mediaRequest.getCreatedBy()));
            mediaRequest.setLastModifiedBy(save(mediaRequest.getLastModifiedBy()));

            return mediaRequestRepository.save(mediaRequest);
        } else {
            return entityManager.merge(mediaRequest);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public MediaLinkedCaseEntity save(MediaLinkedCaseEntity mediaLinkedCaseEntity) {
        mediaLinkedCaseEntity = (MediaLinkedCaseEntity) preCheckPersist(mediaLinkedCaseEntity);

        if (mediaLinkedCaseEntity.getId() == null) {
            mediaLinkedCaseEntity.setCourtCase(save(mediaLinkedCaseEntity.getCourtCase()));
            return mediaLinkedCaseRepository.save(mediaLinkedCaseEntity);
        } else {
            return entityManager.merge(mediaLinkedCaseEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public EventHandlerEntity save(EventHandlerEntity eventHandlerEntity) {
        eventHandlerEntity = (EventHandlerEntity) preCheckPersist(eventHandlerEntity);

        if (eventHandlerEntity.getId() == null) {
            eventHandlerEntity.setCreatedBy(save(eventHandlerEntity.getCreatedBy()));
            return eventHandlerRepository.save(eventHandlerEntity);
        } else {
            return entityManager.merge(eventHandlerEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public CourtCaseEntity save(CourtCaseEntity courtCase) {
        courtCase = (CourtCaseEntity) preCheckPersist(courtCase);

        if (courtCase.getId() == null) {
            courtCase.setCreatedBy(save(courtCase.getCreatedBy()));
            courtCase.setLastModifiedBy(save(courtCase.getLastModifiedBy()));
            courtCase.setCourthouse(save(courtCase.getCourthouse()));

            if (courtCase.getJudges() != null) {
                courtCase.setJudges(saveJudgeList(courtCase.getJudges()));
            }

            if (courtCase.getReportingRestrictions() != null) {
                courtCase.setReportingRestrictions(save(courtCase.getReportingRestrictions()));
            }

            if (courtCase.getDefendantList() != null) {
                courtCase.setDefendantList(saveDefendantList(courtCase.getDefendantList()));
            }

            if (courtCase.getDefenceList() != null) {
                courtCase.setDefenceList(saveDefenceList(courtCase.getDefenceList()));
            }

            if (courtCase.getProsecutorList() != null) {
                courtCase.setProsecutorList(saveProsecutorList(courtCase.getProsecutorList()));
            }

            courtCase = caseRepository.save(courtCase);

        } else {
            courtCase = entityManager.merge(courtCase);
        }

        return courtCase;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public TranscriptionDocumentEntity save(TranscriptionDocumentEntity transcriptionDocumentEntity) {
        transcriptionDocumentEntity = (TranscriptionDocumentEntity) preCheckPersist(transcriptionDocumentEntity);

        if (transcriptionDocumentEntity.getId() == null) {
            transcriptionDocumentEntity.setUploadedBy(save(transcriptionDocumentEntity.getUploadedBy()));
            transcriptionDocumentEntity.setLastModifiedBy(save(transcriptionDocumentEntity.getLastModifiedBy()));
            transcriptionDocumentEntity.setTranscription(save(transcriptionDocumentEntity.getTranscription()));
            if (transcriptionDocumentEntity.getAdminActions() != null) {
                transcriptionDocumentEntity.getAdminActions().forEach(this::save);
            }

            return transcriptionDocumentRepository.save(transcriptionDocumentEntity);
        } else {
            return entityManager.merge(transcriptionDocumentEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public ObjectAdminActionEntity save(ObjectAdminActionEntity adminAction) {
        return objectAdminActionRepository.save(adminAction);
    }


    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public ExternalObjectDirectoryEntity save(ExternalObjectDirectoryEntity eod) {
        eod = (ExternalObjectDirectoryEntity) preCheckPersist(eod);

        if (eod.getId() == null) {
            var locationType = externalLocationTypeRepository.getReferenceById(eod.getExternalLocationType().getId());
            eod.setExternalLocationType(locationType);
            var recordStatus = objectRecordStatusRepository.getReferenceById(eod.getStatus().getId());
            eod.setStatus(recordStatus);

            if (eod.getMedia() != null) {
                eod.setMedia(save(eod.getMedia()));
            }

            if (eod.getAnnotationDocumentEntity() != null) {
                eod.setAnnotationDocumentEntity(save(eod.getAnnotationDocumentEntity()));
            }

            if (eod.getTranscriptionDocumentEntity() != null) {
                eod.setTranscriptionDocumentEntity(save(eod.getTranscriptionDocumentEntity()));
            }

            eod.setCreatedBy(save(eod.getCreatedBy()));
            eod.setLastModifiedBy(save(eod.getLastModifiedBy()));
            return externalObjectDirectoryRepository.save(eod);
        } else {
            return entityManager.merge(eod);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public DefenceEntity save(DefenceEntity defence) {
        defence = (DefenceEntity) preCheckPersist(defence);

        if (defence.getId() == null) {
            defence.setCreatedBy(save(defence.getCreatedBy()));
            defence.setLastModifiedBy(save(defence.getLastModifiedBy()));
            defence.setCourtCase(save(defence.getCourtCase()));
            return defenceRepository.save(defence);
        } else {
            return entityManager.merge(defence);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public DefendantEntity save(DefendantEntity defendant) {
        defendant = (DefendantEntity) preCheckPersist(defendant);

        if (defendant.getId() == null) {
            defendant.setCreatedBy(save(defendant.getCreatedBy()));
            defendant.setLastModifiedBy(save(defendant.getLastModifiedBy()));
            defendant.setCourtCase(save(defendant.getCourtCase()));
            return defendantRepository.save(defendant);
        } else {
            return entityManager.merge(defendant);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public ProsecutorEntity save(ProsecutorEntity prosecutor) {
        prosecutor = (ProsecutorEntity) preCheckPersist(prosecutor);

        if (prosecutor.getId() == null) {
            prosecutor.setCreatedBy(save(prosecutor.getCreatedBy()));
            prosecutor.setLastModifiedBy(save(prosecutor.getCreatedBy()));
            prosecutor.setCourtCase(save(prosecutor.getCourtCase()));
            return prosecutorRepository.save(prosecutor);
        } else {
            return entityManager.merge(prosecutor);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public TranscriptionEntity save(TranscriptionEntity transcription) {
        transcription = (TranscriptionEntity) preCheckPersist(transcription);

        if (transcription.getId() == null) {
            if (transcription.getHearing() != null) {
                transcription.setHearings(saveHearingEntity(transcription.getHearings()));
            }

            if (transcription.getCourtroom() != null) {
                transcription.setCourtroom(save(transcription.getCourtroom()));
            }

            if (transcription.getCourtCases() != null) {
                List<CourtCaseEntity> listOfCases = new ArrayList<>();
                for (CourtCaseEntity courtCase : transcription.getCourtCases()) {
                    listOfCases.add(save(courtCase));
                }
                transcription.setCourtCases(listOfCases);
            }

            saveCreatedBy(transcription);
            saveLastModifiedBy(transcription);
            transcription = transcriptionRepository.save(transcription);

        } else {
            transcription = entityManager.merge(transcription);
        }

        return transcription;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public TranscriptionWorkflowEntity save(TranscriptionWorkflowEntity workflowEntity) {
        workflowEntity = (TranscriptionWorkflowEntity) preCheckPersist(workflowEntity);

        if (workflowEntity.getId() == null) {

            workflowEntity.setTranscription(save(workflowEntity.getTranscription()));

            if (workflowEntity.getTranscriptionComments() != null) {
                workflowEntity.setTranscriptionComments(saceCommentList(workflowEntity.getTranscriptionComments()));
            }

            workflowEntity.setWorkflowActor(save(save(workflowEntity.getWorkflowActor())));
            workflowEntity.setTranscription(save(workflowEntity.getTranscription()));
            workflowEntity.setWorkflowActor(save(workflowEntity.getWorkflowActor()));

            return transcriptionWorkflowRepository.save(workflowEntity);
        } else {
            return entityManager.merge(workflowEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public EventEntity save(EventEntity event) {
        event = (EventEntity) preCheckPersist(event);

        if (event.getId() == null) {
            event.setCourtroom(save(event.getCourtroom()));
            event.setCreatedBy(save(event.getCreatedBy()));
            event.setLastModifiedBy(save(event.getLastModifiedBy()));
            return eventRepository.save(event);
        } else {
            return entityManager.merge(event);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public RetentionPolicyTypeEntity save(RetentionPolicyTypeEntity retentionPolicyType) {
        retentionPolicyType = (RetentionPolicyTypeEntity) preCheckPersist(retentionPolicyType);

        if (retentionPolicyType.getId() == null) {
            retentionPolicyType.setLastModifiedBy(save(retentionPolicyType.getLastModifiedBy()));
            retentionPolicyType.setCreatedBy(save(retentionPolicyType.getCreatedBy()));
            return retentionPolicyTypeRepository.save(retentionPolicyType);
        } else {
            return entityManager.merge(retentionPolicyType);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public CaseManagementRetentionEntity save(CaseManagementRetentionEntity caseManagementRetention) {
        caseManagementRetention = (CaseManagementRetentionEntity) preCheckPersist(caseManagementRetention);

        if (caseManagementRetention.getId() == null) {
            caseManagementRetention.setCourtCase(save(caseManagementRetention.getCourtCase()));
            caseManagementRetention.setEventEntity(save(caseManagementRetention.getEventEntity()));
            caseManagementRetention.setRetentionPolicyTypeEntity(save(caseManagementRetention.getRetentionPolicyTypeEntity()));
            return caseManagementRetentionRepository.save(caseManagementRetention);
        } else {
            return entityManager.merge(caseManagementRetention);
        }
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    @Transactional
    public UserAccountEntity save(UserAccountEntity userAccount) {
        userAccount = (UserAccountEntity) preCheckPersist(userAccount);

        if (userAccount.getId() == null) {
            UserAccountEntity systemUser = userAccountRepository.getReferenceById(0);
            userAccount.setCreatedBy(systemUser);
            userAccount.setLastModifiedBy(systemUser);
            return userAccountRepository.save(userAccount);
        } else {
            return entityManager.merge(userAccount);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public AnnotationDocumentEntity save(AnnotationDocumentEntity annotationDocument) {
        annotationDocument = (AnnotationDocumentEntity) preCheckPersist(annotationDocument);

        if (annotationDocument.getId() == null) {
            annotationDocument.setAnnotation(save(annotationDocument.getAnnotation()));
            annotationDocument.setLastModifiedBy(save(annotationDocument.getLastModifiedBy()));
            annotationDocument.setUploadedBy(save(annotationDocument.getUploadedBy()));
            save(annotationDocument.getAnnotation());
            return annotationDocumentRepository.save(annotationDocument);
        } else {
            return entityManager.merge(annotationDocument);
        }
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    @Transactional
    public JudgeEntity save(JudgeEntity judge) {
        judge = (JudgeEntity) preCheckPersist(judge);

        if (judge.getId() == null) {
            judge.setCreatedBy(save(judge.getCreatedBy()));
            judge.setLastModifiedBy(save(judge.getLastModifiedBy()));

            return judgeRepository.save(judge);
        } else {
            return entityManager.merge(judge);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public MediaEntity save(MediaEntity media) {
        preCheckPersist(media);

        return save(media, true);
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public CaseDocumentEntity save(CaseDocumentEntity caseDocumentEntity) {
        caseDocumentEntity = (CaseDocumentEntity) preCheckPersist(caseDocumentEntity);

        if (caseDocumentEntity.getId() == null) {
            caseDocumentEntity.setCreatedBy(save(caseDocumentEntity.getCreatedBy()));
            caseDocumentEntity.setLastModifiedBy(save(caseDocumentEntity.getLastModifiedBy()));
            caseDocumentEntity.setCourtCase(save(caseDocumentEntity.getCourtCase()));
            return caseDocumentRepository.save(caseDocumentEntity);
        } else {
            return entityManager.merge(caseDocumentEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public MediaEntity save(MediaEntity media, boolean processhHearing) {
        media = (MediaEntity) preCheckPersist(media);

        if (media.getId() == null) {
            media.setCourtroom(save(media.getCourtroom()));
            media.setCreatedBy(save(media.getCreatedBy()));
            media.setLastModifiedBy(save(media.getLastModifiedBy()));

            if (media.getMediaLinkedCaseList() != null) {
                media.setMediaLinkedCaseList(saveLinkedCaseList(media.getMediaLinkedCaseList()));
            }

            media = mediaRepository.save(media);
        } else {
            media = mediaRepository.save(media);
        }
        return media;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public TransformedMediaEntity save(TransformedMediaEntity transformedMedia) {
        transformedMedia = (TransformedMediaEntity) preCheckPersist(transformedMedia);

        if (transformedMedia.getId() == null) {
            transformedMedia.setCreatedBy(save(transformedMedia.getCreatedBy()));
            transformedMedia.setLastModifiedBy(save(transformedMedia.getLastModifiedBy()));
            transformedMedia.setMediaRequest(save(transformedMedia.getMediaRequest()));
            return transformedMediaRepository.save(transformedMedia);
        } else {
            entityManager.merge(transformedMedia);
        }

        return transformedMedia;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public TranscriptionCommentEntity save(TranscriptionCommentEntity commentEntity) {
        commentEntity = (TranscriptionCommentEntity) preCheckPersist(commentEntity);

        if (commentEntity.getId() == null) {
            commentEntity.setCreatedBy(save(commentEntity.getCreatedBy()));
            commentEntity.setLastModifiedBy(save(commentEntity.getLastModifiedBy()));
            commentEntity.setTranscription(save(commentEntity.getTranscription()));
            return transcriptionCommentRepository.save(commentEntity);
        } else {
            entityManager.merge(commentEntity);
        }

        return commentEntity;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public ArmRpoExecutionDetailEntity save(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        armRpoExecutionDetailEntity = (ArmRpoExecutionDetailEntity) preCheckPersist(armRpoExecutionDetailEntity);

        if (armRpoExecutionDetailEntity.getId() == null) {
            armRpoExecutionDetailEntity.setCreatedBy(save(armRpoExecutionDetailEntity.getCreatedBy()));
            armRpoExecutionDetailEntity.setLastModifiedBy(save(armRpoExecutionDetailEntity.getLastModifiedBy()));
            return armRpoExecutionDetailRepository.save(armRpoExecutionDetailEntity);
        } else {
            entityManager.merge(armRpoExecutionDetailEntity);
        }

        return armRpoExecutionDetailEntity;
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public ArmRpoStateEntity save(ArmRpoStateEntity armRpoStateEntity) {
        armRpoStateEntity = (ArmRpoStateEntity) preCheckPersist(armRpoStateEntity);

        if (armRpoStateEntity.getId() == null) {
            return armRpoStateRepository.save(armRpoStateEntity);
        } else {
            return entityManager.merge(armRpoStateEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public ArmRpoStatusEntity save(ArmRpoStatusEntity armRpoStatusEntity) {
        armRpoStatusEntity = (ArmRpoStatusEntity) preCheckPersist(armRpoStatusEntity);

        if (armRpoStatusEntity.getId() == null) {
            return armRpoStatusRepository.save(armRpoStatusEntity);
        } else {
            return entityManager.merge(armRpoStatusEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public void saveAll(UserAccountEntity... userAccounts) {
        stream(userAccounts).forEach(user -> {
            user = (UserAccountEntity) preCheckPersist(user);

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

    @Transactional
    public void saveAll(ExternalObjectDirectoryEntity... externalObjectDirectoryEntities) {
        stream(externalObjectDirectoryEntities).forEach(this::save);
    }

    @Transactional
    public void saveAll(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities) {
        externalObjectDirectoryEntities.forEach(this::save);
    }


    private void saveMediaList(List<MediaEntity> mediaList) {
        mediaList.forEach(media -> {
            if (media.getId() == null) {
                media = (MediaEntity) preCheckPersist(media);
                save(media);
                saveHearingList(media.getHearingList());
            }
        });

    }

    private List<MediaLinkedCaseEntity> saveLinkedCaseList(List<MediaLinkedCaseEntity> linkedCases) {
        List<MediaLinkedCaseEntity> mediaLinkedCaseEntityArrayList = new ArrayList<>();
        linkedCases.forEach(cse -> {
            if (cse.getId() == null) {
                cse = (MediaLinkedCaseEntity) preCheckPersist(cse);

                mediaLinkedCaseEntityArrayList.add(save(cse));
            }
        });

        return mediaLinkedCaseEntityArrayList;
    }

    private List<JudgeEntity> saveJudgeList(List<JudgeEntity> judges) {
        List<JudgeEntity> judgeEntityListReturn = new ArrayList<>();

        judges.forEach(judge -> judgeEntityListReturn.add(save(judge)));

        return judgeEntityListReturn;
    }

    private List<HearingEntity> saveHearingEntity(List<HearingEntity> hearingEntityList) {
        List<HearingEntity> hearingEntityArrayListReturn = new ArrayList<>();

        hearingEntityList.forEach(hearingEntity -> hearingEntityArrayListReturn.add(save(hearingEntity)));

        return hearingEntityArrayListReturn;
    }

    private void saveHearingList(List<HearingEntity> hearings) {
        hearings.forEach(media -> {
            if (media.getId() == null) {
                save(media);
            }
        });
    }

    private List<TranscriptionCommentEntity> saceCommentList(List<TranscriptionCommentEntity> transcriptionCommentEntities) {
        List<TranscriptionCommentEntity> transcriptionWorkflowEntityList = new ArrayList<>();

        transcriptionCommentEntities.forEach(workflowEntity -> transcriptionWorkflowEntityList.add(save(workflowEntity)));

        return transcriptionCommentEntities;
    }

    private List<DefendantEntity> saveDefendantList(List<DefendantEntity> defendantEntities) {
        List<DefendantEntity> defendantEntityList = new ArrayList<>();

        defendantEntities.forEach(defendantEntity -> defendantEntityList.add(saveForCase(defendantEntity)));

        return defendantEntityList;
    }

    private List<DefenceEntity> saveDefenceList(List<DefenceEntity> defenceEntities) {
        List<DefenceEntity> defenceEntityList = new ArrayList<>();

        defenceEntities.forEach(defendantEntity -> defenceEntityList.add(saveForCase(defendantEntity)));

        return defenceEntityList;
    }

    private List<ProsecutorEntity> saveProsecutorList(List<ProsecutorEntity> prosecutorEntities) {
        List<ProsecutorEntity> prosecutorEntityList = new ArrayList<>();

        prosecutorEntities.forEach(prosecutorEntity -> prosecutorEntityList.add(saveForCase(prosecutorEntity)));

        return prosecutorEntityList;
    }

    private Object preCheckPersist(Object objectToSave) {
        // if the entity is a test specific entity then it should not be saved
        if (objectToSave instanceof DbInsertable<?>) {
            return ((DbInsertable<?>) objectToSave).getEntity();
        }
        return objectToSave;
    }


    @SuppressWarnings("PMD.AvoidReassigningParameters")
    private DefenceEntity saveForCase(DefenceEntity defence) {
        defence = (DefenceEntity) preCheckPersist(defence);

        if (defence.getId() == null) {
            defence.setCreatedBy(save(defence.getCreatedBy()));
            defence.setLastModifiedBy(save(defence.getLastModifiedBy()));
        }

        return defence;
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    private DefendantEntity saveForCase(DefendantEntity defendant) {
        defendant = (DefendantEntity) preCheckPersist(defendant);

        defendant.setCreatedBy(save(defendant.getCreatedBy()));
        defendant.setLastModifiedBy(save(defendant.getLastModifiedBy()));

        return defendant;
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    private ProsecutorEntity saveForCase(ProsecutorEntity prosecutor) {
        prosecutor = (ProsecutorEntity) preCheckPersist(prosecutor);

        prosecutor.setCreatedBy(save(prosecutor.getCreatedBy()));
        prosecutor.setLastModifiedBy(save(prosecutor.getCreatedBy()));

        return prosecutor;
    }


    public HearingEntity refresh(HearingEntity entity) {
        return refresh(entity, hearingRepository);
    }

    public UserAccountEntity refresh(UserAccountEntity judge) {
        return refresh(judge, userAccountRepository);
    }


    public CaseRetentionEntity refresh(CaseRetentionEntity caseRetentionEntity) {
        return refresh(caseRetentionEntity, caseRetentionRepository);
    }

    public <T extends HasIntegerId> T refresh(T entity, JpaRepository<T, Integer> repository) {
        if (entity.getId() == null) {
            return entity;
        }
        return repository.findById(entity.getId()).orElseThrow();
    }

    private void saveLastModifiedBy(LastModifiedBy lastModifiedBy) {
        if (lastModifiedBy.getLastModifiedBy() != null) {
            lastModifiedBy.setLastModifiedBy(save(lastModifiedBy.getLastModifiedBy()));
        }
    }

    private void saveCreatedBy(CreatedBy createdBy) {
        if (createdBy.getCreatedBy() != null) {
            createdBy.setCreatedBy(save(createdBy.getCreatedBy()));
        }
    }
}