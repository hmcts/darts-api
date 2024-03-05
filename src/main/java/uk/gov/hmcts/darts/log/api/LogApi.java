package uk.gov.hmcts.darts.log.api;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;

public interface LogApi {
    void eventReceived(DartsEvent event);

    void missingCourthouse(DartsEvent event);

    void missingNodeRegistry(DartsEvent event);

    void processedDailyListJob(DailyListLogJobReport report);

    void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity);

    void audioUploaded(AddAudioMetadataRequest addAudioMetadataRequest);

    void defendantNameOverflow(AddCaseRequest addCaseRequest);

    void casesRequestedByDarPc(GetCasesRequest getCasesRequest);
}