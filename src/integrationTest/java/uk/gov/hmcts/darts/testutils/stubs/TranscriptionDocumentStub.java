package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentSubStringQueryEnum;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;

@Component
@RequiredArgsConstructor
@Deprecated
public class TranscriptionDocumentStub {

    private final TranscriptionStub transcriptionStub;
    private final UserAccountStubComposable userAccountStub;
    private final CourtroomStub courtroomStub;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final UserAccountRepository userAccountRepository;
    private final CourtCaseStub courtCaseStub;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final TranscriptionDocumentStubComposable transcriptionDocumentStubComposable;
    private final DartsDatabaseComposable dartsDatabaseComposable;
    private final CourthouseStubComposable courthouseStub;
    private final TranscriptionStubComposable transcriptionStubComposable;

    /**
     * generates test data. The following will be used for generation:-
     * Unique owner and requested by users for each transcription record
     * Unique court house with unique name for each transcription record
     * Unique case number with unique case number for each transcription record
     * Unique hearing date starting with today with an incrementing day for each transcription record
     * Unique requested date with an incrementing hour for each transcription record
     *
     * @param count                 The number of transcription objects that are to be generated
     * @param hearingCount          The number of hearing against the transcription
     * @param caseCount             The number of cases against the transcription
     * @param isManualTranscription The manual transcription flag
     * @param noCourtHouse          Ensure we do not have a court house against the transcription i.e. use hearing instead
     * @param associatedWorkflow    Whether a workflow is generated against the transcription
     * @return The list of generated media entities in chronological order
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public List<TranscriptionDocumentEntity> generateTranscriptionEntities(int count,
                                                                           int hearingCount,
                                                                           int caseCount,
                                                                           boolean isManualTranscription,
                                                                           boolean noCourtHouse,
                                                                           boolean associatedWorkflow) {

        List<TranscriptionDocumentEntity> retTransformerMediaLst = new ArrayList<>();
        OffsetDateTime hoursBefore = now(UTC);
        OffsetDateTime hoursAfter = now(UTC);
        OffsetDateTime requestedDate = now(UTC);
        LocalDateTime hearingDate = LocalDateTime.now(UTC);

        int fileSize = 1;
        UserAccountEntity owner;
        CourtroomEntity courtroomEntity;
        TranscriptionDocumentEntity transcriptionDocumentEntity;

        List<HearingEntity> hearingEntityList;
        List<CourtCaseEntity> caseEntityList;
        for (int transriptionDocumentCount = 0; transriptionDocumentCount < count; transriptionDocumentCount++) {

            String username = TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryString(Integer.toString(transriptionDocumentCount));
            owner = userAccountStub.createSystemUserAccount(username);

            hearingEntityList = new ArrayList<>();
            caseEntityList = new ArrayList<>();

            // add the cases to the transcription
            CourtCaseEntity caseEntity;
            courtroomEntity = courtroomStub.createCourtroomUnlessExists(
                TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryString(Integer.toString(transriptionDocumentCount)),
                TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                    .getQueryString(UUID.randomUUID() + Integer.toString(transriptionDocumentCount)), userAccountRepository.getReferenceById(0));

            for (int i = 0; i < caseCount; i++) {
                caseEntity = courtCaseStub.createAndSaveMinimalCourtCase(StringUtils.right(TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryString(
                    UUID.randomUUID() + "Case Number" + transriptionDocumentCount + i), 32), courtroomEntity.getCourthouse().getId());
                caseEntityList.add(caseEntity);
            }

            HearingEntity hearingEntity;
            for (int i = 0; i < hearingCount; i++) {
                hearingEntity = retrieveCoreObjectService.retrieveOrCreateHearing(
                    courtroomEntity.getCourthouse().getCourthouseName(),
                    courtroomEntity.getName(),
                    caseEntityList.get(0).getCaseNumber(),
                    hearingDate,
                    userAccountRepository.getReferenceById(0)
                );

                hearingEntityList.add(hearingEntity);
            }

            transcriptionDocumentEntity = new TranscriptionDocumentEntity();
            transcriptionDocumentEntity.setHidden(false);
            transcriptionDocumentEntity.setFileName("File name " + transriptionDocumentCount);
            transcriptionDocumentEntity.setFileType("File type " + transriptionDocumentCount);
            transcriptionDocumentEntity.setFileSize(100);
            transcriptionDocumentEntity.setChecksum("");

            UserAccountEntity requestedBy = userAccountStub.createSystemUserAccount(
                TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(transriptionDocumentCount)));

            transcriptionDocumentEntity.setUploadedBy(requestedBy);
            transcriptionDocumentEntity.setLastModifiedBy(requestedBy);


            List<TranscriptionWorkflowEntity> workflowEntities = new ArrayList<>();
            TranscriptionWorkflowEntity workflowEntity;
            if (associatedWorkflow) {
                workflowEntity = new TranscriptionWorkflowEntity();
                workflowEntity.setWorkflowActor(owner);
                workflowEntity.setTranscriptionStatus(mapToTranscriptionStatusEntity(APPROVED));
                workflowEntity.setWorkflowTimestamp(now());
                workflowEntities.add(workflowEntity);

                workflowEntity = new TranscriptionWorkflowEntity();
                workflowEntity.setWorkflowActor(owner);
                workflowEntity.setTranscriptionStatus(mapToTranscriptionStatusEntity(APPROVED));
                workflowEntity.setWorkflowTimestamp(now());
                workflowEntities.add(workflowEntity);
            }

            TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(hearingEntityList,
                                                                                            caseEntityList,
                                                                                            noCourtHouse ? null : courtroomEntity,
                                                                                            requestedBy, workflowEntities, isManualTranscription);


            transcriptionDocumentEntity.setTranscription(transcriptionEntity);
            transcriptionDocumentRepository.saveAndFlush(transcriptionDocumentEntity);


            fileSize = fileSize + 1;
            hoursBefore = hoursBefore.minusHours(1);
            hoursAfter = hoursAfter.plusHours(1);
            hearingDate = hearingDate.plusDays(count);
            requestedDate = requestedDate.plusDays(1);
            retTransformerMediaLst.add(transcriptionDocumentEntity);
        }

        return retTransformerMediaLst;
    }

    public TranscriptionDocumentEntity createTranscriptionDocumentForTranscription(TranscriptionEntity transcriptionEntity) {
        return transcriptionDocumentStubComposable.createTranscriptionDocumentForTranscription(transcriptionEntity);
    }

    public TranscriptionDocumentEntity createTranscriptionDocumentForTranscription(TranscriptionEntity transcriptionEntity,
                                                                                   UserAccountEntity userAccount) {
        return transcriptionDocumentStubComposable.createTranscriptionDocumentForTranscription(transcriptionEntity, userAccount);
    }

    private TranscriptionStatusEntity mapToTranscriptionStatusEntity(TranscriptionStatusEnum statusEnum) {
        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(statusEnum.getId());
        transcriptionStatus.setStatusType(statusEnum.name());
        transcriptionStatus.setDisplayName(statusEnum.name());
        return transcriptionStatus;
    }


}