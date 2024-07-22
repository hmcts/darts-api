package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyListHouseKeepingAutomatedTaskTest {

    @Mock
    private DailyListService dailyListService;

    @Mock
    private LogApi logApi;

    @Mock
    private LockService lockService;

    @Test
    void runTask() {
        var dailyListAutomatedTask = new DailyListAutomatedTask(
                null,
                null,
                dailyListService,
                logApi,
                lockService
        );

        dailyListAutomatedTask.runTask();
        verify(dailyListService, times(1)).runHouseKeeping();
    }
}