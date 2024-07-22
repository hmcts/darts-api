package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.service.LockService;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LockServiceImpl implements LockService {

    private static final int DEFAULT_LOCK_AT_MOST_SECONDS = 600;
    private static final int DEFAULT_LOCK_AT_LEAST_SECONDS = 20;

    private final AutomatedTaskRepository automatedTaskRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final LockProvider lockProvider;

    private LockingTaskExecutor lockingTaskExecutor;

    @Override
    public LockingTaskExecutor getLockingTaskExecutor() {
        if (lockingTaskExecutor == null) {
            lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);
        }
        return lockingTaskExecutor;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofSeconds(DEFAULT_LOCK_AT_MOST_SECONDS);
    }

    @Override
    public Duration getLockAtLeastFor() {
        return Duration.ofSeconds(DEFAULT_LOCK_AT_LEAST_SECONDS);
    }

    @Override
    public boolean isLocked(AutomatedTaskEntity automatedTask) {
        List<Timestamp> lockedUntil = automatedTaskRepository.findLockedUntilForTask(automatedTask.getTaskName());
        return !lockedUntil.isEmpty() && isInFuture(lockedUntil);
    }

    private boolean isInFuture(List<Timestamp> lockedUntil) {
        // There should only ever be one item in the list as we search by primary key
        return toOffsetDateTime(lockedUntil.get(0)).isAfter(currentTimeHelper.currentOffsetDateTime());
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC);
    }
}
