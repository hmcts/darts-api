package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;

import static uk.gov.hmcts.darts.event.mapper.TranscriptionRequestDetailsMapper.transcriptionRequestDetailsFrom;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@Slf4j
@Service
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class SentencingRemarksAndRetentionPolicyHandler extends EventHandlerBase {

    private final TranscriptionsApi transcriptionsApi;

    public SentencingRemarksAndRetentionPolicyHandler(RetrieveCoreObjectService retrieveCoreObjectService,
          EventRepository eventRepository,
          HearingRepository hearingRepository,
          CaseRepository caseRepository,
          ApplicationEventPublisher eventPublisher,
          TranscriptionsApi transcriptionsApi) {
        super(eventRepository, hearingRepository, caseRepository, eventPublisher, retrieveCoreObjectService);
        this.transcriptionsApi = transcriptionsApi;
    }

    @Override
    @Transactional
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        var transcriptionRequestDetails = transcriptionRequestDetailsFrom(
              dartsEvent,
              createHearingAndSaveEvent(dartsEvent, eventHandler).getHearingEntity());

        transcriptionRequestDetails.setTranscriptionTypeId(SENTENCING_REMARKS.getId());
        transcriptionRequestDetails.setTranscriptionUrgencyId(STANDARD.getId());

        var transcriptionResponse = transcriptionsApi.saveTranscriptionRequest(transcriptionRequestDetails, false);

        var updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(APPROVED.getId());
        updateTranscription.setWorkflowComment("Transcription Automatically approved");
        transcriptionsApi.updateTranscription(transcriptionResponse.getTranscriptionId(), updateTranscription);
    }

}
