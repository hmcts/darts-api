package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;

import static uk.gov.hmcts.darts.event.mapper.TranscriptionRequestDetailsMapper.transcriptionRequestDetailsFrom;

@Slf4j
@Service
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class TranscriptionRequestHandler extends EventHandlerBase {

    private final TranscriptionsApi transcriptionsApi;

    public TranscriptionRequestHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                                       EventRepository eventRepository,
                                       HearingRepository hearingRepository,
                                       CaseRepository caseRepository,
                                       ApplicationEventPublisher eventPublisher,
                                       TranscriptionsApi transcriptionsApi,
                                       AuthorisationApi authorisationApi) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, authorisationApi);
        this.transcriptionsApi = transcriptionsApi;
    }

    @Override
    @Transactional
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        //save the event in the database
        var createdHearing = createHearingAndSaveEvent(dartsEvent, eventHandler);
        var transcriptionRequestDetails = transcriptionRequestDetailsFrom(dartsEvent, createdHearing.getHearingEntity());

        //create automatic transcription request
        transcriptionRequestDetails.setTranscriptionTypeId(TranscriptionTypeEnum.OTHER.getId());
        transcriptionRequestDetails.setTranscriptionUrgencyId(TranscriptionUrgencyEnum.OVERNIGHT.getId());
        RequestTranscriptionResponse transcriptionResponse = transcriptionsApi.saveTranscriptionRequest(
            transcriptionRequestDetails, false);

        //automatically approve the transcription request
        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(TranscriptionStatusEnum.APPROVED.getId());
        updateTranscription.setWorkflowComment("Transcription Automatically approved");
        transcriptionsApi.updateTranscription(transcriptionResponse.getTranscriptionId(), updateTranscription);
    }

}
