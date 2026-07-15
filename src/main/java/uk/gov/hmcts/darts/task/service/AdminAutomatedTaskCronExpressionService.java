package uk.gov.hmcts.darts.task.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.task.config.AdminAutomatedTaskCronExpressionConfig;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionSchedule;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError.AUTOMATED_TASK_CRON_EXPRESSION_BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AdminAutomatedTaskCronExpressionService {

    private final AdminAutomatedTaskCronExpressionConfig adminAutomatedTaskCronExpressionConfig;

    public List<AutomatedTaskCronExpressionSchedule> getCronExpressionSchedulePreview(String cronExpression) {
        List<AutomatedTaskCronExpressionSchedule> scheduledRunTimes = new ArrayList<>();

        CronExpression cron = parseCronExpression(cronExpression);
        OffsetDateTime next = OffsetDateTime.now();
        int maxExecutionCount = adminAutomatedTaskCronExpressionConfig.getExecutionCount();

        for (int executionNumber = 1; executionNumber <= maxExecutionCount; executionNumber++) {
            assert next != null;
            next = cron.next(next);
            scheduledRunTimes.add(createScheduledRun(executionNumber, next));
        }

        return scheduledRunTimes;
    }

    public void validateCronExpression(String cronExpression) {
        parseCronExpression(cronExpression);
    }

    private static AutomatedTaskCronExpressionSchedule createScheduledRun(int executionNumber, OffsetDateTime scheduledAt) {
        AutomatedTaskCronExpressionSchedule scheduledRun = new AutomatedTaskCronExpressionSchedule();
        scheduledRun.setExecutionNumber(String.valueOf(executionNumber));
        scheduledRun.setScheduledAt(scheduledAt);
        return scheduledRun;
    }

    private static CronExpression parseCronExpression(String cronExpression) {
        try {
            return CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new DartsApiException(AUTOMATED_TASK_CRON_EXPRESSION_BAD_REQUEST, e);
        }
    }
}
