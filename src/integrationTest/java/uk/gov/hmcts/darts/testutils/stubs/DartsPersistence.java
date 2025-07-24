package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
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
import uk.gov.hmcts.darts.task.runner.HasId;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.builder.DbInsertable;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@SuppressWarnings({
    "PMD.ExcessiveImports", "PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity",
    "PMD.AvoidReassigningParameters"})
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
            retentionConfidenceCategoryMapperEntity = retentionConfidenceCategoryMapperRepository.save(retentionConfidenceCategoryMapperEntity);
        } else {
            retentionConfidenceCategoryMapperEntity = entityManager.merge(retentionConfidenceCategoryMapperEntity);
        }

        return retentionConfidenceCategoryMapperEntity;
    }

    @Transactional
    public HearingEntity save(HearingEntity hearing) {
        hearing = (HearingEntity) preCheckPersist(hearing);
        hearing.setJudges(saveJudgeList(hearing.getJudges()));
        hearing.setCourtCase(save(hearing.getCourtCase()));
        hearing.setCourtroom(save(hearing.getCourtroom()));
        saveMediaList(hearing.getMedias());
        hearing.setCreatedById(Optional.ofNullable(hearing.getCreatedById()).orElse(0));
        hearing.setLastModifiedById(Optional.ofNullable(hearing.getLastModifiedById()).orElse(0));

        hearing = hearingRepository.save(hearing);

        return hearing;
    }

    @Transactional
    public AnnotationEntity save(AnnotationEntity annotationEntity) {
        annotationEntity = (AnnotationEntity) preCheckPersist(annotationEntity);

        if (annotationEntity.getId() == null) {
            annotationEntity.setCurrentOwner(save(annotationEntity.getCurrentOwner()));
            annotationEntity.setCreatedById(Optional.ofNullable(annotationEntity.getCreatedById()).orElse(0));
            annotationEntity.setLastModifiedById(Optional.ofNullable(annotationEntity.getLastModifiedById()).orElse(0));
            if (annotationEntity.getHearings() != null) {
                annotationEntity.setHearings(saveHearingEntity(annotationEntity.getHearings()));
            }
            return annotationRepository.save(annotationEntity);
        } else {
            return entityManager.merge(annotationEntity);
        }
    }

    @Transactional
    public CourthouseEntity save(CourthouseEntity courthouse) {
        courthouse = (CourthouseEntity) preCheckPersist(courthouse);

        if (courthouse.getId() == null) {
            courthouse.setCreatedById(Optional.ofNullable(courthouse.getCreatedById()).orElse(0));
            courthouse.setLastModifiedById(Optional.ofNullable(courthouse.getLastModifiedById()).orElse(0));
            courthouseRepository.save(courthouse);
        } else {
            courthouse = entityManager.merge(courthouse);
        }

        return courthouse;
    }

    @Transactional
    public CourtroomEntity save(CourtroomEntity courtroom) {
        courtroom = (CourtroomEntity) preCheckPersist(courtroom);

        if (courtroom.getId() == null) {
            if (isNull(courtroom.getCourthouse().getId())) {
                courtroom.setCourthouse(save(courtroom.getCourthouse()));
            }
            courtroom.setCreatedById(Optional.ofNullable(courtroom.getCreatedById()).orElse(0));
            return courtroomRepository.save(courtroom);
        } else {
            return entityManager.merge(courtroom);
        }
    }

    @Transactional
    public MediaRequestEntity save(MediaRequestEntity mediaRequest) {
        mediaRequest = (MediaRequestEntity) preCheckPersist(mediaRequest);

        if (mediaRequest.getId() == null) {
            mediaRequest.setHearing(save(mediaRequest.getHearing()));
            mediaRequest.setRequestor(save(mediaRequest.getRequestor()));
            mediaRequest.setCurrentOwner(save(mediaRequest.getCurrentOwner()));
            mediaRequest.setCreatedById(Optional.ofNullable(mediaRequest.getCreatedById()).orElse(0));
            mediaRequest.setLastModifiedById(Optional.ofNullable(mediaRequest.getLastModifiedById()).orElse(0));

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
    public EventHandlerEntity save(EventHandlerEntity eventHandlerEntity) {
        eventHandlerEntity = (EventHandlerEntity) preCheckPersist(eventHandlerEntity);

        if (eventHandlerEntity.getId() == null) {
            eventHandlerEntity.setCreatedById(Optional.ofNullable(eventHandlerEntity.getCreatedById()).orElse(0));
            return eventHandlerRepository.save(eventHandlerEntity);
        } else {
            return entityManager.merge(eventHandlerEntity);
        }
    }

    @Transactional
    public NodeRegisterEntity save(NodeRegisterEntity nodeRegisterEntity) {
        nodeRegisterEntity = (NodeRegisterEntity) preCheckPersist(nodeRegisterEntity);

        if (nodeRegisterEntity.getNodeId() == null) {
            if (isNull(nodeRegisterEntity.getCourtroom().getId())) {
                nodeRegisterEntity.setCourtroom(save(nodeRegisterEntity.getCourtroom()));
            }
            nodeRegisterEntity.setCreatedById(Optional.ofNullable(nodeRegisterEntity.getCreatedById()).orElse(0));
            return nodeRegisterRepository.save(nodeRegisterEntity);
        } else {
            return entityManager.merge(nodeRegisterEntity);
        }
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public CourtCaseEntity save(CourtCaseEntity courtCase) {
        courtCase = (CourtCaseEntity) preCheckPersist(courtCase);

        if (courtCase.getId() == null) {
            courtCase.setCreatedById(Optional.ofNullable(courtCase.getCreatedById()).orElse(0));
            courtCase.setLastModifiedById(Optional.ofNullable(courtCase.getLastModifiedById()).orElse(0));
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
    public TranscriptionDocumentEntity save(TranscriptionDocumentEntity transcriptionDocumentEntity) {
        transcriptionDocumentEntity = (TranscriptionDocumentEntity) preCheckPersist(transcriptionDocumentEntity);

        if (transcriptionDocumentEntity.getId() == null) {
            transcriptionDocumentEntity.setUploadedBy(save(transcriptionDocumentEntity.getUploadedBy()));
            transcriptionDocumentEntity.setLastModifiedById(Optional.ofNullable(transcriptionDocumentEntity.getLastModifiedById()).orElse(0));
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
    public ObjectAdminActionEntity save(ObjectAdminActionEntity adminAction) {
        if (adminAction.getAnnotationDocument() != null) {
            save(adminAction.getAnnotationDocument());
        }
        if (adminAction.getCaseDocument() != null) {
            save(adminAction.getCaseDocument());
        }
        if (adminAction.getMedia() != null) {
            save(adminAction.getMedia());
        }
        if (adminAction.getTranscriptionDocument() != null) {
            save(adminAction.getTranscriptionDocument());
        }

        return objectAdminActionRepository.save(adminAction);
    }


    @Transactional
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

            eod.setCreatedById(Optional.ofNullable(eod.getCreatedById()).orElse(0));
            eod.setLastModifiedById(Optional.ofNullable(eod.getLastModifiedById()).orElse(0));
            return externalObjectDirectoryRepository.save(eod);
        } else {
            return entityManager.merge(eod);
        }
    }

    @Transactional
    public DefenceEntity save(DefenceEntity defence) {
        defence = (DefenceEntity) preCheckPersist(defence);

        if (defence.getId() == null) {
            defence.setCreatedById(Optional.ofNullable(defence.getCreatedById()).orElse(0));
            defence.setLastModifiedById(Optional.ofNullable(defence.getLastModifiedById()).orElse(0));
            defence.setCourtCase(save(defence.getCourtCase()));
            return defenceRepository.save(defence);
        } else {
            return entityManager.merge(defence);
        }
    }

    @Transactional
    public DefendantEntity save(DefendantEntity defendant) {
        defendant = (DefendantEntity) preCheckPersist(defendant);

        if (defendant.getId() == null) {
            defendant.setCreatedById(Optional.ofNullable(defendant.getCreatedById()).orElse(0));
            defendant.setLastModifiedById(Optional.ofNullable(defendant.getLastModifiedById()).orElse(0));
            defendant.setCourtCase(save(defendant.getCourtCase()));
            return defendantRepository.save(defendant);
        } else {
            return entityManager.merge(defendant);
        }
    }

    @Transactional
    public ProsecutorEntity save(ProsecutorEntity prosecutor) {
        prosecutor = (ProsecutorEntity) preCheckPersist(prosecutor);

        if (prosecutor.getId() == null) {
            prosecutor.setCreatedById(Optional.ofNullable(prosecutor.getCreatedById()).orElse(0));
            prosecutor.setLastModifiedById(Optional.ofNullable(prosecutor.getLastModifiedById()).orElse(0));
            prosecutor.setCourtCase(save(prosecutor.getCourtCase()));
            return prosecutorRepository.save(prosecutor);
        } else {
            return entityManager.merge(prosecutor);
        }
    }

    @Transactional
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
                Set<CourtCaseEntity> listOfCases = new HashSet<>();
                for (CourtCaseEntity courtCase : transcription.getCourtCases()) {
                    listOfCases.add(save(courtCase));
                }
                transcription.setCourtCases(listOfCases);
            }
            transcription = transcriptionRepository.save(transcription);

        } else {
            transcription = entityManager.merge(transcription);
        }

        return transcription;
    }

    @Transactional
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
    public EventEntity save(EventEntity event) {
        event = (EventEntity) preCheckPersist(event);

        if (event.getId() == null) {
            event.setCourtroom(save(event.getCourtroom()));
            event.setCreatedById(Optional.ofNullable(event.getCreatedById()).orElse(0));
            event.setLastModifiedById(Optional.ofNullable(event.getLastModifiedById()).orElse(0));
            return eventRepository.save(event);
        } else {
            return entityManager.merge(event);
        }
    }

    @Transactional
    public RetentionPolicyTypeEntity save(RetentionPolicyTypeEntity retentionPolicyType) {
        retentionPolicyType = (RetentionPolicyTypeEntity) preCheckPersist(retentionPolicyType);

        if (retentionPolicyType.getId() == null) {
            retentionPolicyType.setLastModifiedById(Optional.ofNullable(retentionPolicyType.getLastModifiedById()).orElse(0));
            retentionPolicyType.setCreatedById(Optional.ofNullable(retentionPolicyType.getCreatedById()).orElse(0));
            return retentionPolicyTypeRepository.save(retentionPolicyType);
        } else {
            return entityManager.merge(retentionPolicyType);
        }
    }

    @Transactional
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
    public AnnotationDocumentEntity save(AnnotationDocumentEntity annotationDocument) {
        annotationDocument = (AnnotationDocumentEntity) preCheckPersist(annotationDocument);

        if (annotationDocument.getId() == null) {
            annotationDocument.setAnnotation(save(annotationDocument.getAnnotation()));
            annotationDocument.setLastModifiedById(Optional.ofNullable(annotationDocument.getLastModifiedById()).orElse(0));
            annotationDocument.setUploadedBy(save(annotationDocument.getUploadedBy()));
            save(annotationDocument.getAnnotation());
            return annotationDocumentRepository.save(annotationDocument);
        } else {
            return entityManager.merge(annotationDocument);
        }
    }

    @Transactional
    public JudgeEntity save(JudgeEntity judge) {
        judge = (JudgeEntity) preCheckPersist(judge);
        if (judge.getId() == null) {
            judge.setCreatedById(Optional.ofNullable(judge.getCreatedById()).orElse(0));
            judge.setLastModifiedById(Optional.ofNullable(judge.getLastModifiedById()).orElse(0));

            return judgeRepository.save(judge);
        } else {
            return entityManager.merge(judge);
        }
    }

    @Transactional
    public MediaEntity save(MediaEntity media) {
        preCheckPersist(media);

        return save(media, true);
    }

    @Transactional
    public CaseDocumentEntity save(CaseDocumentEntity caseDocumentEntity) {
        caseDocumentEntity = (CaseDocumentEntity) preCheckPersist(caseDocumentEntity);

        if (caseDocumentEntity.getId() == null) {
            caseDocumentEntity.setCreatedById(Optional.ofNullable(caseDocumentEntity.getCreatedById()).orElse(0));
            caseDocumentEntity.setLastModifiedById(Optional.ofNullable(caseDocumentEntity.getLastModifiedById()).orElse(0));
            caseDocumentEntity.setCourtCase(save(caseDocumentEntity.getCourtCase()));
            return caseDocumentRepository.save(caseDocumentEntity);
        } else {
            return entityManager.merge(caseDocumentEntity);
        }
    }

    @Transactional
    public MediaEntity save(MediaEntity media, boolean processhHearing) {
        media = (MediaEntity) preCheckPersist(media);

        if (media.getId() == null) {
            media.setCourtroom(save(media.getCourtroom()));
            media.setCreatedById(Optional.ofNullable(media.getCreatedById()).orElse(0));
            media.setLastModifiedById(Optional.ofNullable(media.getLastModifiedById()).orElse(0));

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
    public TransformedMediaEntity save(TransformedMediaEntity transformedMedia) {
        transformedMedia = (TransformedMediaEntity) preCheckPersist(transformedMedia);

        if (transformedMedia.getId() == null) {
            transformedMedia.setCreatedById(Optional.ofNullable(transformedMedia.getCreatedById()).orElse(0));
            transformedMedia.setLastModifiedById(Optional.ofNullable(transformedMedia.getLastModifiedById()).orElse(0));
            transformedMedia.setMediaRequest(save(transformedMedia.getMediaRequest()));
            return transformedMediaRepository.save(transformedMedia);
        } else {
            entityManager.merge(transformedMedia);
        }

        return transformedMedia;
    }

    @Transactional
    public TranscriptionCommentEntity save(TranscriptionCommentEntity commentEntity) {
        commentEntity = (TranscriptionCommentEntity) preCheckPersist(commentEntity);

        if (commentEntity.getId() == null) {
            commentEntity.setCreatedById(Optional.ofNullable(commentEntity.getCreatedById()).orElse(0));
            commentEntity.setLastModifiedById(Optional.ofNullable(commentEntity.getLastModifiedById()).orElse(0));
            commentEntity.setTranscription(save(commentEntity.getTranscription()));
            return transcriptionCommentRepository.save(commentEntity);
        } else {
            entityManager.merge(commentEntity);
        }

        return commentEntity;
    }

    @Transactional
    public ArmRpoExecutionDetailEntity save(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        armRpoExecutionDetailEntity = (ArmRpoExecutionDetailEntity) preCheckPersist(armRpoExecutionDetailEntity);

        if (armRpoExecutionDetailEntity.getId() == null) {
            armRpoExecutionDetailEntity.setCreatedById(Optional.ofNullable(armRpoExecutionDetailEntity.getCreatedById()).orElse(0));
            armRpoExecutionDetailEntity.setLastModifiedById(Optional.ofNullable(armRpoExecutionDetailEntity.getLastModifiedById()).orElse(0));
            return armRpoExecutionDetailRepository.save(armRpoExecutionDetailEntity);
        } else {
            entityManager.merge(armRpoExecutionDetailEntity);
        }

        return armRpoExecutionDetailEntity;
    }

    @Transactional
    public ArmRpoStateEntity save(ArmRpoStateEntity armRpoStateEntity) {
        armRpoStateEntity = (ArmRpoStateEntity) preCheckPersist(armRpoStateEntity);

        if (armRpoStateEntity.getId() == null) {
            return armRpoStateRepository.save(armRpoStateEntity);
        } else {
            return entityManager.merge(armRpoStateEntity);
        }
    }

    @Transactional
    public ArmRpoStatusEntity save(ArmRpoStatusEntity armRpoStatusEntity) {
        armRpoStatusEntity = (ArmRpoStatusEntity) preCheckPersist(armRpoStatusEntity);

        if (armRpoStatusEntity.getId() == null) {
            return armRpoStatusRepository.save(armRpoStatusEntity);
        } else {
            return entityManager.merge(armRpoStatusEntity);
        }
    }

    @Transactional
    public SecurityGroupEntity save(SecurityGroupEntity securityGroupEntity) {
        securityGroupEntity = (SecurityGroupEntity) preCheckPersist(securityGroupEntity);

        if (securityGroupEntity.getId() == null) {
            securityGroupEntity.setCreatedById(Optional.ofNullable(securityGroupEntity.getCreatedById()).orElse(0));
            securityGroupEntity.setLastModifiedById(Optional.ofNullable(securityGroupEntity.getLastModifiedById()).orElse(0));
            return securityGroupRepository.save(securityGroupEntity);
        } else {
            entityManager.merge(securityGroupEntity);
        }

        return securityGroupEntity;
    }

    @Transactional
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
    public void saveAll(EventEntity... events) {
        stream(events).forEach(this::save);
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
    public void saveAll(MediaEntity... mediaEntities) {
        stream(mediaEntities).forEach(this::save);
    }

    @Transactional
    public void saveAll(ObjectAdminActionEntity... adminActionEntities) {
        stream(adminActionEntities).forEach(this::save);
    }

    @Transactional
    public void saveAll(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities) {
        externalObjectDirectoryEntities.forEach(this::save);
    }

    private void saveMediaList(Collection<MediaEntity> mediaList) {
        if (mediaList == null
            || TestUtils.isProxy(mediaList)
            || mediaList.isEmpty()) {
            return;
        }
        mediaList.forEach(media -> {
            if (media.getId() == null) {
                media = (MediaEntity) preCheckPersist(media);
                save(media);
                saveHearing(media.getHearings());
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

    private Set<JudgeEntity> saveJudgeList(Collection<JudgeEntity> judges) {
        Set<JudgeEntity> judgeEntityListReturn = new HashSet<>();

        judges.forEach(judge -> judgeEntityListReturn.add(save(judge)));

        return judgeEntityListReturn;
    }

    private Set<HearingEntity> saveHearingEntity(Collection<HearingEntity> hearingEntityList) {
        Set<HearingEntity> newCollection = new HashSet<>();
        hearingEntityList.forEach(hearingEntity -> newCollection.add(save(hearingEntity)));
        return newCollection;
    }

    private void saveHearing(Collection<HearingEntity> hearings) {
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

    private DefenceEntity saveForCase(DefenceEntity defence) {
        defence = (DefenceEntity) preCheckPersist(defence);

        if (defence.getId() == null) {
            defence.setCreatedById(Optional.ofNullable(defence.getCreatedById()).orElse(0));
            defence.setLastModifiedById(Optional.ofNullable(defence.getLastModifiedById()).orElse(0));
        }

        return defence;
    }

    private DefendantEntity saveForCase(DefendantEntity defendant) {
        defendant = (DefendantEntity) preCheckPersist(defendant);

        defendant.setCreatedById(Optional.ofNullable(defendant.getCreatedById()).orElse(0));
        defendant.setLastModifiedById(Optional.ofNullable(defendant.getLastModifiedById()).orElse(0));

        return defendant;
    }

    private ProsecutorEntity saveForCase(ProsecutorEntity prosecutor) {
        prosecutor = (ProsecutorEntity) preCheckPersist(prosecutor);

        prosecutor.setCreatedById(Optional.ofNullable(prosecutor.getCreatedById()).orElse(0));
        prosecutor.setLastModifiedById(Optional.ofNullable(prosecutor.getLastModifiedById()).orElse(0));

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

    @Transactional
    public void overrideLastModifiedBy(MediaRequestEntity mediaRequestEntity, OffsetDateTime lastModifiedDate) {
        entityManager.createNativeQuery("UPDATE media_request SET last_modified_ts = :lastModifiedDate WHERE mer_id = :id")
            .setParameter("lastModifiedDate", lastModifiedDate)
            .setParameter("id", mediaRequestEntity.getId())
            .executeUpdate();
    }

    @Transactional
    public <T extends HasId<?> & CreatedBy> void updateCreatedBy(T object, OffsetDateTime createdDateTime) {
        Table table = object.getClass().getAnnotation(Table.class);

        entityManager.createNativeQuery("UPDATE darts." + table.name() + " set created_ts = ? where " + getIdColumnName(object) + " = ?")
            .setParameter(1, createdDateTime)
            .setParameter(2, object.getId())
            .executeUpdate();
    }

    private String getIdColumnName(HasId<?> object) {
        return stream(object.getClass().getDeclaredFields())
            .filter(field -> field.getAnnotation(Id.class) != null)
            .map(field -> field.getAnnotation(Column.class))
            .findFirst()
            .orElseThrow()
            .name();
    }
}