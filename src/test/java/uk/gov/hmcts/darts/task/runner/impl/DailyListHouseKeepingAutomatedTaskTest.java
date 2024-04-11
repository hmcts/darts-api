package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;
import uk.gov.hmcts.darts.log.api.LogApi;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyListHouseKeepingAutomatedTaskTest {

    @Mock
    private LockProvider lockProvider;
    @Mock
    private DailyListService dailyListService;

    @Mock
    private LogApi logApi;

    @Test
    void runTask() {
        var dailyListAutomatedTask = new DailyListAutomatedTask(
                null,
                lockProvider,
                null,
                dailyListService,
                logApi
        );

        dailyListAutomatedTask.runTask();
        verify(dailyListService, times(1)).runHouseKeeping();
    }
}