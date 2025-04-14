package uk.gov.hmcts.darts.log.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.log.service.ArmLoggerService;
import uk.gov.hmcts.darts.log.service.AtsLoggerService;
import uk.gov.hmcts.darts.log.service.AudioLoggerService;
import uk.gov.hmcts.darts.log.service.AutomatedTaskLoggerService;
import uk.gov.hmcts.darts.log.service.CasesLoggerService;
import uk.gov.hmcts.darts.log.service.DailyListLoggerService;
import uk.gov.hmcts.darts.log.service.DeletionLoggerService;
import uk.gov.hmcts.darts.log.service.EventLoggerService;
import uk.gov.hmcts.darts.log.service.impl.NotificationLoggerService;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.service.notify.NotificationClientException;

import java.time.Duration;
import java.time.OffsetDateTime;
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
    private final ArmLoggerService armLoggerService;
    private final DeletionLoggerService deletionLoggerService;

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

    @Override
    public void defendantNotAdded(String defendant, String caseNumber) {
        casesLoggerService.defendantNotAdded(defendant, caseNumber);
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
    public void errorRetryingNotification(NotificationEntity notification, String templateId, NotificationClientException ex) {
        notificationLoggerService.errorRetryingNotification(notification, templateId, ex);
    }

    @Override
    public void failedNotification(NotificationEntity notification, String templateId, NotificationClientException ex) {
        notificationLoggerService.failedNotification(notification, templateId, ex);
    }

    @Override
    public void taskStarted(UUID taskExecutionId, String taskName, Integer batchSize) {
        automatedTaskLoggerService.taskStarted(taskExecutionId, taskName, batchSize);
    }

    @Override
    public void taskCompleted(UUID taskExecutionId, String taskName) {
        automatedTaskLoggerService.taskCompleted(taskExecutionId, taskName);
    }

    @Override
    public void taskFailed(UUID taskExecutionId, String taskName) {
        automatedTaskLoggerService.taskFailed(taskExecutionId, taskName);
    }

    @Override
    public void armPushSuccessful(Integer eodId) {
        armLoggerService.armPushSuccessful(eodId);
    }

    @Override
    public void armPushFailed(Integer eodId) {
        armLoggerService.armPushFailed(eodId);
    }

    @Override
    public void archiveToArmSuccessful(Integer eodId) {
        armLoggerService.archiveToArmSuccessful(eodId);
    }

    @Override
    public void archiveToArmFailed(Integer eodId) {
        armLoggerService.archiveToArmFailed(eodId);
    }

    @Override
    public void caseDeletedDueToExpiry(Integer caseId, String caseNumber) {
        casesLoggerService.caseDeletedDueToExpiry(caseId, caseNumber);
    }

    @Override
    public void mediaDeleted(Integer mediaId) {
        deletionLoggerService.mediaDeleted(mediaId);
    }

    @Override
    public void transcriptionDeleted(Integer transcriptionId) {
        deletionLoggerService.transcriptionDeleted(transcriptionId);
    }

    @Override
    public void logArmMissingResponse(Duration armMissingResponseDuration, Integer eodId) {
        armLoggerService.logArmMissingResponse(armMissingResponseDuration, eodId);
    }

    @Override
    public void manualObfuscation(EventEntity eventEntity) {
        eventLoggerService.manualObfuscation(eventEntity);
    }

    @Override
    public void armRpoSearchSuccessful(Integer executionId) {
        armLoggerService.armRpoSearchSuccessful(executionId);
    }

    @Override
    public void armRpoSearchFailed(Integer executionId) {
        armLoggerService.armRpoSearchFailed(executionId);
    }

    @Override
    public void armRpoPollingSuccessful(Integer executionId) {
        armLoggerService.armRpoPollingSuccessful(executionId);
    }

    @Override
    public void armRpoPollingFailed(Integer executionId) {
        armLoggerService.armRpoPollingFailed(executionId);
    }

    @Override
    public void addAudioSmallFileWithLongDuration(String courthouse, String courtroom, OffsetDateTime startDate, OffsetDateTime finishDate,
                                                  Integer medId, Long fileSize) {
        audioLoggerService.addAudioSmallFileWithLongDuration(courthouse, courtroom, startDate, finishDate, medId, fileSize);
    }

}