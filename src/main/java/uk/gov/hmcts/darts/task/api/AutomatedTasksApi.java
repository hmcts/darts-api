package uk.gov.hmcts.darts.task.api;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

import java.time.Duration;
import java.util.Optional;

public interface AutomatedTasksApi {

    Optional<AutomatedTaskEntity> getTaskByName(String taskName);

    boolean isLocked(AutomatedTaskEntity automatedTaskEntity);

    LockingTaskExecutor getLockingTaskExecutor();

    Duration getLockAtMostFor();

    Duration getLockAtLeastFor();
}
