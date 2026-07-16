package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.task.config.AdminAutomatedTaskCronExpressionConfig;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionSchedule;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminAutomatedTasksServiceHelperTest {

    private final AdminAutomatedTaskCronExpressionConfig config = new AdminAutomatedTaskCronExpressionConfig();

    @BeforeEach
    void setUp() {
        config.setExecutionCount(3);
    }

    @Test
    void getCronExpressionSchedulePreviewReturnsConfiguredNumberOfFutureRunTimes() {
        AdminAutomatedTasksServiceHelper helper = createHelper("2026-07-16T08:30:00Z");

        List<AutomatedTaskCronExpressionSchedule> schedule = helper.getCronExpressionSchedulePreview("0 0 10 * * *");

        assertEquals(3, schedule.size());
        assertScheduledRun(schedule.get(0), "1", "2026-07-16T10:00:00+01:00");
        assertScheduledRun(schedule.get(1), "2", "2026-07-17T10:00:00+01:00");
        assertScheduledRun(schedule.get(2), "3", "2026-07-18T10:00:00+01:00");
    }

    @Test
    void getCronExpressionSchedulePreviewUsesEuropeLondonDaylightSavingsRules() {
        config.setExecutionCount(2);
        AdminAutomatedTasksServiceHelper helper = createHelper("2026-10-24T12:00:00Z");

        List<AutomatedTaskCronExpressionSchedule> schedule = helper.getCronExpressionSchedulePreview("0 0 0 * * *");

        assertEquals(2, schedule.size());
        assertScheduledRun(schedule.get(0), "1", "2026-10-25T00:00:00+01:00");
        assertScheduledRun(schedule.get(1), "2", "2026-10-26T00:00:00Z");
    }

    @Test
    void validateAndParseCronExpressionReturnsParsedCronExpressionWhenValid() {
        AdminAutomatedTasksServiceHelper helper = createHelper("2026-07-16T08:30:00Z");

        assertNotNull(helper.validateAndParseCronExpression("0 0 10 * * *"));
    }

    @Test
    void validateAndParseCronExpressionThrowsBadRequestWhenInvalid() {
        AdminAutomatedTasksServiceHelper helper = createHelper("2026-07-16T08:30:00Z");

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> helper.validateAndParseCronExpression("not a cron expression")
        );

        assertEquals(AutomatedTaskApiError.AUTOMATED_TASK_BAD_REQUEST, exception.getError());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    private AdminAutomatedTasksServiceHelper createHelper(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new AdminAutomatedTasksServiceHelper(config, clock);
    }

    private static void assertScheduledRun(
        AutomatedTaskCronExpressionSchedule scheduledRun,
        String executionNumber,
        String scheduledAt) {
        assertEquals(executionNumber, scheduledRun.getExecutionNumber());
        assertEquals(OffsetDateTime.parse(scheduledAt), scheduledRun.getScheduledAt());
    }
}
