package uk.gov.hmcts.darts.task.runner.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.authentication.component.DartsJwt;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AbstractAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingAutomatedTask;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.validation.constraints.NotNull;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.COMPLETED;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.FAILED;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.IN_PROGRESS;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.LOCK_FAILED;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.NOT_STARTED;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.SKIPPED;

@Slf4j
public abstract class AbstractLockableAutomatedTask<T extends AbstractAutomatedTaskConfig> implements AutomatedTask, AutoloadingAutomatedTask {

    private AutomatedTaskStatus automatedTaskStatus = NOT_STARTED;

    private String lastCronExpression;

    private final AutomatedTaskRepository automatedTaskRepository;

    private Instant start = Instant.now();

    private final T automatedTaskConfigurationProperties;

    private final LogApi logApi;

    private final LockService lockService;

    private ThreadLocal<UUID> executionId;
    @Getter
    private boolean isManualRun;
    @Autowired
    private UserAccountRepository userAccountRepository;

    protected AbstractLockableAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                            T abstractAutomatedTaskConfig,
                                            LogApi logApi, LockService lockService) {
        this.automatedTaskRepository = automatedTaskRepository;
        this.automatedTaskConfigurationProperties = abstractAutomatedTaskConfig;
        this.logApi = logApi;
        this.lockService = lockService;
    }

    protected T getConfig() {
        return this.automatedTaskConfigurationProperties;
    }

    private void setupUserAuthentication() {
        String email = automatedTaskConfigurationProperties.getSystemUserEmail();
        Integer userId = userAccountRepository.findFirstByEmailAddressIgnoreCase(email)
            .orElseThrow(() -> new DartsException(
                "System user not found with email " + email))
            .getId();

        DartsJwt jwt = new DartsJwt(Jwt.withTokenValue("automated-task")
                                        .header("alg", "RS256")
                                        .claim("emails", List.of(email))
                                        .build(),
                                    userId);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        log.debug("Task: {} using email: {}", getTaskName(), email);
    }

    @Override
    public void run(boolean isManualRun) {
        this.isManualRun = isManualRun;
        executionId = ThreadLocal.withInitial(UUID::randomUUID);
        preRunTask();
        try {
            Optional<AutomatedTaskEntity> automatedTaskEntity = automatedTaskRepository.findByTaskName(getTaskName());
            if (automatedTaskEntity.isPresent()) {
                AutomatedTaskEntity automatedTask = automatedTaskEntity.get();
                String dbCronExpression = automatedTask.getCronExpression();
                // Check the cron expression hasn't been changed in the database by another instance, if so skip this run
                if (isManualRun || getLastCronExpression().equals(dbCronExpression)) {
                    if (isManualRun || TRUE.equals(automatedTask.getTaskEnabled())) {
                        if (!TRUE.equals(automatedTask.getTaskEnabled())) {
                            log.info("Task: {} is inactive but has been run manually", getTaskName());
                        }
                        logApi.taskStarted(executionId.get(), this.getTaskName());
                        lockService.getLockingTaskExecutor().executeWithLock(createLockableTask(), getLockConfiguration());
                    } else {
                        setAutomatedTaskStatus(SKIPPED);
                        log.warn("Task: {} not running now as it has been disabled", getTaskName());
                    }
                } else {
                    setAutomatedTaskStatus(SKIPPED);
                    log.warn("Task: {} not running now as cron expression has been changed in the database from '{}' to '{}'",
                             getTaskName(), getLastCronExpression(), dbCronExpression
                    );
                }
            }
        } catch (Exception exception) {
            logApi.taskFailed(executionId.get(), getTaskName());
            setAutomatedTaskStatus(FAILED);
            log.error("Task: {} exception while attempting to start the task", getTaskName(), exception);
        } finally {
            postRunTask();
        }
    }

    LockedTask createLockableTask() {
        return new LockedTask();
    }

    @Override
    public AutomatedTaskStatus getAutomatedTaskStatus() {
        return automatedTaskStatus;
    }

    @Override
    public String getLastCronExpression() {
        return lastCronExpression;
    }

    @Override
    public void setLastCronExpression(@NotNull String cronExpression) {
        this.lastCronExpression = cronExpression;
    }

    protected void setAutomatedTaskStatus(AutomatedTaskStatus automatedTaskStatus) {
        log.debug("Task: {} changing status from {} to {}", getTaskName(), this.automatedTaskStatus, automatedTaskStatus);
        this.automatedTaskStatus = automatedTaskStatus;
    }

    protected abstract void runTask();

    public Duration getLockAtMostFor() {
        return Optional.ofNullable(automatedTaskConfigurationProperties.getLock())
            .map(AbstractAutomatedTaskConfig.Lock::getAtMostFor)
            .filter(Duration::isPositive)
            .orElseGet(lockService::getLockAtMostFor);
    }

    public Duration getLockAtLeastFor() {
        return Optional.ofNullable(automatedTaskConfigurationProperties.getLock())
            .map(lock -> lock.getAtLeastFor())
            .filter(duration -> duration.isPositive())
            .orElseGet(() -> lockService.getLockAtLeastFor());
    }

    protected void handleException(Exception exception) {
        log.error("Task: {} exception during execution of the task business logic", getTaskName(), exception);
    }

    protected Optional<AutomatedTaskEntity> getAutomatedTaskDetails(String taskName) {
        return automatedTaskRepository.findByTaskName(taskName);
    }

    protected Integer getAutomatedTaskBatchSize() {
        return getAutomatedTaskBatchSize(getTaskName(), 0);
    }

    protected Integer getAutomatedTaskBatchSize(String taskName) {
        return getAutomatedTaskBatchSize(taskName, 0);
    }

    protected Integer getAutomatedTaskBatchSize(int defaultBatchSize) {
        return getAutomatedTaskBatchSize(getTaskName(), defaultBatchSize);
    }

    protected Integer getAutomatedTaskBatchSize(String taskName, int defaultBatchSize) {
        Integer batchSize = defaultBatchSize;
        Optional<AutomatedTaskEntity> automatedTaskEntity = getAutomatedTaskDetails(taskName);
        if (automatedTaskEntity.isPresent()) {
            AutomatedTaskEntity automatedTask = automatedTaskEntity.get();
            if (nonNull(automatedTask.getBatchSize())) {
                batchSize = automatedTask.getBatchSize();
            }
        }
        return batchSize;
    }

    private LockConfiguration getLockConfiguration() {
        return new LockConfiguration(
            Instant.now(),
            getTaskName(),
            this.getLockAtMostFor(),
            this.getLockAtLeastFor()
        );
    }

    void preRunTask() {
        setupUserAuthentication();
        start = Instant.now();
        log.info("Task: {} potential candidate for execution at: {}", getTaskName(), LocalDateTime.now());
        setAutomatedTaskStatus(IN_PROGRESS);
    }

    private void postRunTask() {
        stopStopwatchAndLogFinished();
        if (getAutomatedTaskStatus().equals(IN_PROGRESS)) {
            setAutomatedTaskStatus(COMPLETED);
            logApi.taskCompleted(executionId.get(), this.getTaskName());
        }
        executionId.remove();
    }

    private void stopStopwatchAndLogFinished() {
        Instant finish = Instant.now();
        Duration timeElapsedDuration = Duration.between(start, finish);
        long timeElapsed = timeElapsedDuration.toMillis();
        log.info("Task: {} finished running at: {}", getTaskName(), LocalDateTime.now());
        log.info("Task: {} time elapsed: {} ms", getTaskName(), timeElapsed);
        if (lockService.getLockAtMostFor().compareTo(timeElapsedDuration) < 0) {
            log.warn("Task: {} started at {} and finished at {}, taking {}ms, which is longer than the max locking time of {}",
                     getTaskName(), start, finish, timeElapsed, lockService.getLockAtMostFor());
        }
    }

    @Override
    public AbstractLockableAutomatedTask getAbstractLockableAutomatedTask() {
        return this;
    }

    @Override
    public String getTaskName() {
        return getAutomatedTaskName().getTaskName();
    }

    @Override
    public abstract AutomatedTaskName getAutomatedTaskName();


    class LockedTask implements Runnable {
        @Override
        public void run() {
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                try {
                    assertLocked();
                } catch (IllegalStateException exception) {
                    setAutomatedTaskStatus(LOCK_FAILED);
                    log.error("Unable to lock task", exception);
                }

                final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                Future<?> future = executor.submit(() -> {
                    try {
                        //Spring security context default strategy is ThreadLocal meaning we need to set it up on each thread we want the user on
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        runTask();
                    } catch (Exception exception) {
                        setAutomatedTaskStatus(FAILED);
                        handleException(exception);
                        throw exception;
                    }
                });

                try {
                    future.get(getLockAtMostFor().toMillis(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    log.error("Task: {} timed out after {}ms", getTaskName(), getLockAtMostFor().toMillis());
                    future.cancel(true);
                } catch (ExecutionException e) {
                    log.error("Task: {} execution exception", getTaskName(), e);
                } catch (InterruptedException e) {
                    log.error("Task: {} interrupted", getTaskName(), e);
                    Thread.currentThread().interrupt();
                }
                executor.shutdown();
            }
        }

        //Separate method to allow mocking
        void assertLocked() {
            LockAssert.assertLocked();
        }
    }
}
