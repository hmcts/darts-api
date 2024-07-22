package uk.gov.hmcts.darts.task.service;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

import java.time.Duration;

public interface LockService {
    boolean isLocked(AutomatedTaskEntity automatedTask);

    LockingTaskExecutor getLockingTaskExecutor();

    Duration getLockAtMostFor();

    Duration getLockAtLeastFor();
}
