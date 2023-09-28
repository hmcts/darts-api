package uk.gov.hmcts.darts.transcriptions.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_TRANSCRIPTION_STATUS;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TranscriptionServiceImpl implements TranscriptionService {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final UserAccountRepository userAccountRepository;

    private final CaseService caseService;
    private final HearingsService hearingsService;

    @Transactional
    @Override
    public void saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails) {

        UserAccountEntity userAccount = getUserAccount();
        TranscriptionStatusEntity transcriptionStatus = getTranscriptionStatusById(REQUESTED.getId());

        TranscriptionEntity transcription = saveTranscription(
            userAccount,
            transcriptionRequestDetails,
            transcriptionStatus,
            getTranscriptionType(transcriptionRequestDetails.getTranscriptionTypeId()),
            getTranscriptionUrgencyById(transcriptionRequestDetails.getUrgencyId())
        );

        saveTranscriptionWorkflow(
            userAccount,
            transcription,
            transcriptionStatus,
            transcriptionRequestDetails.getComment()
        );
    }

    @Override
    @Transactional
    public UpdateTranscriptionResponse updateTranscription(Integer transcriptionId,
                                                           UpdateTranscription updateTranscription) {

        TranscriptionEntity transcription = transcriptionRepository.findById(transcriptionId)
            .orElseThrow(() -> new DartsApiException(TRANSCRIPTION_NOT_FOUND));

        Map<TranscriptionStatusEnum, Set<TranscriptionStatusEnum>> expectedStatuses = new HashMap<>();
        expectedStatuses.put(AWAITING_AUTHORISATION, Set.of(APPROVED, REJECTED));

        TranscriptionStatusEnum transcriptionStatusEnum = TranscriptionStatusEnum.fromId(updateTranscription.getTranscriptionStatusId());
        Set<TranscriptionStatusEnum> allowed = expectedStatuses.get(
            TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId()));
        if (allowed == null || !allowed.contains(transcriptionStatusEnum)) {
            throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_STATUS);
        }

        TranscriptionWorkflowEntity transcriptionWorkflowEntity = saveTranscriptionWorkflow(
            getUserAccount(),
            transcription,
            getTranscriptionStatusById(transcriptionStatusEnum.getId()),
            updateTranscription.getWorkflowComment()
        );

        UpdateTranscriptionResponse updateTranscriptionResponse = new UpdateTranscriptionResponse();
        updateTranscriptionResponse.setTranscriptionWorkflowId(transcriptionWorkflowEntity.getId());
        return updateTranscriptionResponse;
    }

    private TranscriptionEntity saveTranscription(UserAccountEntity userAccount,
                                                  TranscriptionRequestDetails transcriptionRequestDetails,
                                                  TranscriptionStatusEntity transcriptionStatus,
                                                  TranscriptionTypeEntity transcriptionType,
                                                  TranscriptionUrgencyEntity transcriptionUrgency) {

        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        }

        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionType(transcriptionType);
        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setStart(transcriptionRequestDetails.getStartDateTime());
        transcription.setEnd(transcriptionRequestDetails.getEndDateTime());
        transcription.setCreatedBy(userAccount);
        transcription.setLastModifiedBy(userAccount);

        if (nonNull(transcriptionRequestDetails.getCaseId())) {
            transcription.setCourtCase(caseService.getCourtCaseById(transcriptionRequestDetails.getCaseId()));
        }

        if (nonNull(transcriptionRequestDetails.getHearingId())) {
            HearingEntity hearing = hearingsService.getHearingById(transcriptionRequestDetails.getHearingId());
            transcription.setHearing(hearing);
            if (isNull(transcription.getCourtCase())) {
                transcription.setCourtCase(hearing.getCourtCase());
            }
        }

        return transcriptionRepository.saveAndFlush(transcription);
    }

    private TranscriptionWorkflowEntity saveTranscriptionWorkflow(UserAccountEntity userAccount,
                                                                  TranscriptionEntity transcription,
                                                                  TranscriptionStatusEntity transcriptionStatus,
                                                                  String workflowComment) {

        TranscriptionWorkflowEntity transcriptionWorkflow = new TranscriptionWorkflowEntity();
        transcriptionWorkflow.setTranscription(transcription);
        transcriptionWorkflow.setTranscriptionStatus(transcriptionStatus);
        transcriptionWorkflow.setWorkflowActor(userAccount);
        transcriptionWorkflow.setWorkflowTimestamp(OffsetDateTime.now(UTC));
        transcriptionWorkflow.setWorkflowComment(workflowComment);
        transcriptionWorkflow.setCreatedBy(userAccount);
        transcriptionWorkflow.setLastModifiedBy(userAccount);

        return transcriptionWorkflowRepository.saveAndFlush(transcriptionWorkflow);
    }

    // TODO This needs to be replaced by the actual users account when this has been implemented
    private UserAccountEntity getUserAccount() {
        return userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
    }

    private TranscriptionUrgencyEntity getTranscriptionUrgencyById(Integer urgencyId) {
        return transcriptionUrgencyRepository.getReferenceById(urgencyId);
    }

    private TranscriptionStatusEntity getTranscriptionStatusById(Integer transcriptionStatusId) {
        return transcriptionStatusRepository.getReferenceById(transcriptionStatusId);
    }

    private TranscriptionTypeEntity getTranscriptionType(Integer transcriptionTypeId) {
        return transcriptionTypeRepository.getReferenceById(transcriptionTypeId);
    }

}
