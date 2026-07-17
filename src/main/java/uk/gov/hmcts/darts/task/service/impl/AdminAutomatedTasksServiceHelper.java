package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionSchedule;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.DateConverterUtil.EUROPE_LONDON_ZONE;
import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AdminAutomatedTasksServiceHelper {

    private static final int CRON_SCHEDULE_PREVIEW_COUNT = 10;

    private final Clock clock;

    public List<AutomatedTaskCronExpressionSchedule> getCronExpressionSchedulePreview(String cronExpression) {
        List<AutomatedTaskCronExpressionSchedule> scheduledRunTimes = new ArrayList<>();

        CronExpression cron = validateAndParseCronExpression(cronExpression);
        ZonedDateTime next = ZonedDateTime.ofInstant(clock.instant(), EUROPE_LONDON_ZONE);

        for (int executionNumber = 1; executionNumber <= CRON_SCHEDULE_PREVIEW_COUNT; executionNumber++) {
            next = cron.next(next);
            assert next != null;
            scheduledRunTimes.add(createScheduledRun(executionNumber, next.toOffsetDateTime()));
        }

        return scheduledRunTimes;
    }

    private static AutomatedTaskCronExpressionSchedule createScheduledRun(int executionNumber, OffsetDateTime scheduledAt) {
        AutomatedTaskCronExpressionSchedule scheduledRun = new AutomatedTaskCronExpressionSchedule();
        scheduledRun.setExecutionNumber(String.valueOf(executionNumber));
        scheduledRun.setScheduledAt(scheduledAt);
        return scheduledRun;
    }

    public CronExpression validateAndParseCronExpression(String cronExpression) {
        try {
            return CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new DartsApiException(AUTOMATED_TASK_BAD_REQUEST, e);
        }
    }
}
