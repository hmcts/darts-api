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
import uk.gov.hmcts.darts.common.enums.TranscriptionWorkflowStageEnum;
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
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import static java.util.Objects.isNull;

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

        TranscriptionUrgencyEntity transcriptionUrgency = getTranscriptionUrgencyById(transcriptionRequestDetails.getUrgencyId());
        TranscriptionStatusEntity transcriptionStatus = getTranscriptionStatusById(TranscriptionStatusEnum.REQUESTED);
        TranscriptionTypeEntity transcriptionType = getTranscriptionType(transcriptionRequestDetails.getTranscriptionTypeId());
        TranscriptionEntity transcription = saveTranscription(
            userAccount,
            transcriptionRequestDetails,
            transcriptionUrgency,
            transcriptionStatus,
            transcriptionType
        );

        saveTranscriptionWorkflow(
            userAccount,
            transcriptionRequestDetails,
            transcription,
            TranscriptionWorkflowStageEnum.REQUESTED
        );
    }

    private TranscriptionEntity saveTranscription(UserAccountEntity userAccount,
                                                  TranscriptionRequestDetails transcriptionRequestDetails,
                                                  TranscriptionUrgencyEntity transcriptionUrgency,
                                                  TranscriptionStatusEntity transcriptionStatus,
                                                  TranscriptionTypeEntity transcriptionType
    ) {
        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        }

        TranscriptionEntity transcription = new TranscriptionEntity();
        HearingEntity hearing = null;
        if (!isNull(transcriptionRequestDetails.getHearingId())) {
            hearing = hearingsService.getHearingById(transcriptionRequestDetails.getHearingId());
            transcription.setHearing(hearing);
        }
        if (!isNull(transcriptionRequestDetails.getCaseId())) {
            transcription.setCourtCase(caseService.getCourtCaseById(transcriptionRequestDetails.getCaseId()));
        } else if (!isNull(hearing)) {
            transcription.setCourtCase(hearing.getCourtCase());
        } else {
            throw new DartsApiException(TranscriptionError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        }

        transcription.setStart(transcriptionRequestDetails.getStartDateTime());
        transcription.setEnd(transcriptionRequestDetails.getEndDateTime());

        transcription.setTranscriptionUrgency(transcriptionUrgency);
        transcription.setTranscriptionStatus(transcriptionStatus);
        transcription.setTranscriptionType(transcriptionType);

        transcription.setCreatedBy(userAccount);
        transcription.setLastModifiedBy(userAccount);

        return transcriptionRepository.saveAndFlush(transcription);
    }

    private void saveTranscriptionWorkflow(UserAccountEntity userAccount,
                                           TranscriptionRequestDetails transcriptionRequestDetails,
                                           TranscriptionEntity transcription,
                                           TranscriptionWorkflowStageEnum workflowStageEnum) {
        TranscriptionWorkflowEntity transcriptionWorkflow = new TranscriptionWorkflowEntity();

        transcriptionWorkflow.setTranscription(transcription);
        transcriptionWorkflow.setWorkflowComment(transcriptionRequestDetails.getComment());
        transcriptionWorkflow.setWorkflowStage(workflowStageEnum);

        transcriptionWorkflow.setCreatedBy(userAccount);
        transcriptionWorkflow.setLastModifiedBy(userAccount);

        transcriptionWorkflowRepository.saveAndFlush(transcriptionWorkflow);
    }

    // TODO This needs to be replaced by the actual users account when this has been implemented
    private UserAccountEntity getUserAccount() {
        return userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
    }

    private TranscriptionUrgencyEntity getTranscriptionUrgencyById(Integer urgencyId) {
        return transcriptionUrgencyRepository.getReferenceById(urgencyId);
    }

    private TranscriptionStatusEntity getTranscriptionStatusById(TranscriptionStatusEnum transcriptionStatusEnum) {
        return transcriptionStatusRepository.getReferenceById(transcriptionStatusEnum.getId());
    }

    private TranscriptionTypeEntity getTranscriptionType(Integer transcriptionTypeId) {
        return transcriptionTypeRepository.getReferenceById(transcriptionTypeId);
    }

}
