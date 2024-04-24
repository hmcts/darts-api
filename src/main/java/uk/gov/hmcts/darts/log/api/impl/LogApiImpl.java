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
import uk.gov.hmcts.darts.log.service.AutomatedTaskLoggerService;
import uk.gov.hmcts.darts.log.service.CasesLoggerService;
import uk.gov.hmcts.darts.log.service.DailyListLoggerService;
import uk.gov.hmcts.darts.log.service.EventLoggerService;
import uk.gov.hmcts.darts.log.service.impl.NotificationLoggerService;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.service.notify.NotificationClientException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogApiImpl implements LogApi {

    private final EventLoggerService eventLoggerService;
    private final AtsLoggerService atsLoggerService;
    private final CasesLoggerService casesLoggerService;
    private final AudioLoggerService audioLoggerService;
    private final NotificationLoggerService notificationLoggerService;
    private final DailyListLoggerService logJobService;
    private final AutomatedTaskLoggerService automatedTaskLoggerService;

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
    public void processedDailyListJob(DailyListLogJobReport report) {
        logJobService.logJobReport(report);
    }

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

    @Override
    public void scheduleNotification(NotificationEntity notificationEntity, Integer caseId) {
        notificationLoggerService.scheduleNotification(notificationEntity, caseId);
    }

    @Override
    public void sendingNotification(NotificationEntity notification, String templateId, Integer attempts) {
        notificationLoggerService.sendingNotification(notification, templateId, attempts);
    }

    @Override
    public void sentNotification(NotificationEntity notification, String templateId, Integer attempts) {
        notificationLoggerService.sentNotification(notification, templateId, attempts);
    }

    @Override
    public void errorRetryingNotification(NotificationEntity notification, String templateId, NotificationClientException e) {
        notificationLoggerService.errorRetryingNotification(notification, templateId, e);
    }

    @Override
    public void failedNotification(NotificationEntity notification, String templateId, NotificationClientException e) {
        notificationLoggerService.failedNotification(notification, templateId, e);
    }

    @Override
    public void taskStarted(UUID taskExecutionId, String taskName) {
        automatedTaskLoggerService.taskStarted(taskExecutionId, taskName);
    }

    @Override
    public void taskCompleted(UUID taskExecutionId, String taskName) {
        automatedTaskLoggerService.taskCompleted(taskExecutionId, taskName);
    }

    @Override
    public void taskFailed(UUID taskExecutionId, String taskName) {
        automatedTaskLoggerService.taskFailed(taskExecutionId, taskName);
    }
}

