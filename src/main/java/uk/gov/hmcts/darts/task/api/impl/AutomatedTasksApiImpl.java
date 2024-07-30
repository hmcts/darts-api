package uk.gov.hmcts.darts.task.api.impl;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.api.AutomatedTasksApi;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutomatedTasksApiImpl implements AutomatedTasksApi {

    private final AutomatedTaskRepository automatedTaskRepository;
    private final LockService lockService;

    @Override
    public Optional<AutomatedTaskEntity> getTaskByName(String taskName) {
        return automatedTaskRepository.findByTaskName(taskName);
    }

    @Override
    public boolean isLocked(AutomatedTaskEntity automatedTaskEntity) {
        return lockService.isLocked(automatedTaskEntity);
    }

    @Override
    public LockingTaskExecutor getLockingTaskExecutor() {
        return lockService.getLockingTaskExecutor();
    }

    @Override
    public Duration getLockAtMostFor() {
        return lockService.getLockAtMostFor();
    }

    @Override
    public Duration getLockAtLeastFor() {
        return lockService.getLockAtLeastFor();
    }
}
