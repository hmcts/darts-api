package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionScheduleResponse;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.DateConverterUtil.EUROPE_LONDON_ZONE;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_CRON_EXPRESSION_INVALID;

@Service
@RequiredArgsConstructor
public class AdminAutomatedTasksServiceHelper {

    @Value("${darts.automated.task.admin-edit-cron-expression.cron-expression-preview-count}")
    private final int cronExpressionPreviewCount;

    private final Clock clock;

    public List<AutomatedTaskCronExpressionScheduleResponse> getCronExpressionSchedulePreview(CronExpression cron) {
        List<AutomatedTaskCronExpressionScheduleResponse> scheduleResponseObject = new ArrayList<>();

        ZonedDateTime next = ZonedDateTime.ofInstant(clock.instant(), EUROPE_LONDON_ZONE);

        for (int executionNumber = 1; executionNumber <= cronExpressionPreviewCount; executionNumber++) {
            next = cron.next(next);
            scheduleResponseObject.add(createExecutionTimeObject(executionNumber, next.toOffsetDateTime()));
        }

        return scheduleResponseObject;
    }

    private static AutomatedTaskCronExpressionScheduleResponse createExecutionTimeObject(int executionNumber, OffsetDateTime scheduledAt) {
        AutomatedTaskCronExpressionScheduleResponse executionTimeObject = new AutomatedTaskCronExpressionScheduleResponse();
        executionTimeObject.setExecutionNumber(String.valueOf(executionNumber));
        executionTimeObject.setScheduledAt(scheduledAt);
        return executionTimeObject;
    }

    public CronExpression validateAndParseCronExpression(String cronExpression) {
        try {
            return CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new DartsApiException(AUTOMATED_TASK_CRON_EXPRESSION_INVALID, e);
        }
    }
}
