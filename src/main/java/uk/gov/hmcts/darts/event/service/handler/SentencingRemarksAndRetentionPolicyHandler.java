package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.CaseManagementRetentionService;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;
import uk.gov.hmcts.darts.util.DataUtil;

import static uk.gov.hmcts.darts.event.mapper.TranscriptionRequestDetailsMapper.transcriptionRequestDetailsFrom;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@Slf4j
@Service
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class SentencingRemarksAndRetentionPolicyHandler extends EventHandlerBase {

    private final TranscriptionsApi transcriptionsApi;

    private final CaseManagementRetentionService caseManagementRetentionService;

    public SentencingRemarksAndRetentionPolicyHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                                                      EventRepository eventRepository,
                                                      HearingRepository hearingRepository,
                                                      CaseRepository caseRepository,
                                                      ApplicationEventPublisher eventPublisher,
                                                      TranscriptionsApi transcriptionsApi,
                                                      LogApi logApi,
                                                      CaseManagementRetentionService caseManagementRetentionService,
                                                      EventPersistenceService eventPersistenceService) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, logApi, eventPersistenceService);
        this.transcriptionsApi = transcriptionsApi;
        this.caseManagementRetentionService = caseManagementRetentionService;
    }

    @Override
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        DataUtil.preProcess(dartsEvent);
        var hearingAndEvent = createHearingAndSaveEvent(dartsEvent, eventHandler);

        var transcriptionRequestDetails = transcriptionRequestDetailsFrom(
            dartsEvent,
            hearingAndEvent.getHearingEntity());

        transcriptionRequestDetails.setTranscriptionTypeId(SENTENCING_REMARKS.getId());
        transcriptionRequestDetails.setTranscriptionUrgencyId(STANDARD.getId());

        try {
            var transcriptionResponse = transcriptionsApi.saveTranscriptionRequest(transcriptionRequestDetails, false);
            var updateTranscription = new UpdateTranscriptionRequest();
            updateTranscription.setTranscriptionStatusId(APPROVED.getId());
            updateTranscription.setWorkflowComment("Transcription Automatically approved");
            transcriptionsApi.updateTranscription(transcriptionResponse.getTranscriptionId(), updateTranscription);

        } catch (DartsApiException dartsException) {
            //duplicate transcriptions can be safely ignored.
            if (dartsException.getError().equals(TranscriptionApiError.DUPLICATE_TRANSCRIPTION)) {
                log.warn("EventId {} caused a duplicate Transcription Request, which has been ignored.", hearingAndEvent.getEventEntity().getId());
            } else {
                throw dartsException;
            }

        }

        // store retention information for potential future use
        if (dartsEvent.getRetentionPolicy() != null) {
            caseManagementRetentionService.createCaseManagementRetention(
                hearingAndEvent.getEventEntity(),
                hearingAndEvent.getHearingEntity().getCourtCase(),
                dartsEvent.getRetentionPolicy());
        }
    }

}
