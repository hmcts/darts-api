package uk.gov.hmcts.darts.log.api;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.service.notify.NotificationClientException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public interface LogApi {
    void eventReceived(DartsEvent event);

    void missingCourthouse(DartsEvent event);

    void missingNodeRegistry(DartsEvent event);

    void processedDailyListJob(DailyListLogJobReport report);

    void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity);

    void audioUploaded(AddAudioMetadataRequest addAudioMetadataRequest);

    void defendantNameOverflow(AddCaseRequest addCaseRequest);

    void defendantNotAdded(String defendant, String caseNumber);

    void casesRequestedByDarPc(GetCasesRequest getCasesRequest);

    void scheduleNotification(NotificationEntity notificationEntity, Integer caseId);

    void sendingNotification(NotificationEntity notification, String templateId, Integer attempts);

    void sentNotification(NotificationEntity notification, String templateId, Integer attempts);

    void errorRetryingNotification(NotificationEntity notification, String templateId, NotificationClientException ex);

    void failedNotification(NotificationEntity notification, String templateId, NotificationClientException ex);

    void taskStarted(UUID taskExecutionId, String taskName, Integer batchSize);

    void taskCompleted(UUID taskExecutionId, String taskName);

    void taskFailed(UUID taskExecutionId, String taskName);

    void armPushSuccessful(Long eodId);

    void armPushFailed(Long eodId);

    void archiveToArmSuccessful(Long eodId);

    void archiveToArmFailed(Long eodId);

    void caseDeletedDueToExpiry(Integer caseId, String caseNumber);

    void manualObfuscation(EventEntity eventEntity);

    void mediaDeleted(Long mediaId);

    void transcriptionDeleted(Long transcriptionId);

    void logArmMissingResponse(Duration armMissingResponseDuration, Long id);

    void armRpoSearchSuccessful(Integer executionId);

    void armRpoSearchFailed(Integer executionId);

    void armRpoPollingSuccessful(Integer executionId);

    void armRpoPollingFailed(Integer executionId);

    void addAudioSmallFileWithLongDuration(String courthouse, String courtroom, OffsetDateTime startDate, OffsetDateTime finishDate,
                                           Long medId, Long fileSize);
    
    void removeOldArmRpoProductionsSuccessful(Integer executionId);
    
    void removeOldArmRpoProductionsFailed();

    void removeOldArmRpoProductionsFailed(Integer executionId);
}