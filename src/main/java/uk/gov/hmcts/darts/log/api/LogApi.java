package uk.gov.hmcts.darts.log.api;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.service.notify.NotificationClientException;

public interface LogApi {
    void eventReceived(DartsEvent event);

    void missingCourthouse(DartsEvent event);

    void missingNodeRegistry(DartsEvent event);

    void processedDailyListJob(DailyListLogJobReport report);

    void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity);

    void audioUploaded(AddAudioMetadataRequest addAudioMetadataRequest);

    void defendantNameOverflow(AddCaseRequest addCaseRequest);

    void casesRequestedByDarPc(GetCasesRequest getCasesRequest);

    void scheduleNotification(NotificationEntity notificationEntity, Integer caseId);

    void sendingNotification(NotificationEntity notification, String templateId, Integer attempts);

    void sentNotification(NotificationEntity notification, String templateId, Integer attempts);

    void errorRetryingNotification(NotificationEntity notification, String templateId, NotificationClientException e);

    void failedNotification(NotificationEntity notification, String templateId, NotificationClientException e);


}