package uk.gov.hmcts.darts.log.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.log.service.AtsLoggerService;
import uk.gov.hmcts.darts.log.service.AudioLoggerService;
import uk.gov.hmcts.darts.log.service.CasesLoggerService;
import uk.gov.hmcts.darts.log.service.EventLoggerService;

@Service
@RequiredArgsConstructor
public class LogApiImpl implements LogApi {

    private final EventLoggerService eventLoggerService;
    private final AtsLoggerService atsLoggerService;
    private final CasesLoggerService casesLoggerService;
    private final AudioLoggerService audioLoggerService;


    @Override
    public void eventReceived(DartsEvent event) {
        eventLoggerService.eventReceived(event);
    }

    @Override
    public void missingCourthouse(DartsEvent event) {
        eventLoggerService.missingCourthouse(event);
    }

    @Override
    public void missingNodeRegistry(DartsEvent event) {
        eventLoggerService.missingNodeRegistry(event);
    }

    @Override
    public void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity) {
        atsLoggerService.atsProcessingUpdate(mediaRequestEntity);
    }

    @Override
    public void casesRequestedByDarPc(GetCasesRequest getCasesRequest) {
        casesLoggerService.casesRequestedByDarPc(getCasesRequest);
    }

    @Override
    public void audioUploaded(AddAudioMetadataRequest addAudioMetadataRequest) {
        audioLoggerService.audioUploaded(addAudioMetadataRequest);
    }

    @Override
    public void defendantNameOverflow(AddCaseRequest addCaseRequest) {
        casesLoggerService.defendantNameOverflow(addCaseRequest);
    }
}
