package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.validation.constraints.NotNull;

import static java.lang.Boolean.TRUE;


@Slf4j
public abstract class AbstractLockableAutomatedTask implements AutomatedTask {

    public static final int DEFAULT_LOCK_AT_MOST_SECONDS = 600;
    public static final int DEFAULT_LOCK_AT_LEAST_SECONDS = 20;

    private AutomatedTaskStatus automatedTaskStatus = AutomatedTaskStatus.NOT_STARTED;

    private String lastCronExpression;

    private final AutomatedTaskRepository automatedTaskRepository;

    private Instant start = Instant.now();

    private final LockingTaskExecutor lockingTaskExecutor;

    private final AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;

    private final LogApi logApi;

    private ThreadLocal<UUID> executionId;

    protected AbstractLockableAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                            AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties, LogApi logApi) {
        this.automatedTaskRepository = automatedTaskRepository;
        this.lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);
        this.automatedTaskConfigurationProperties = automatedTaskConfigurationProperties;
        this.logApi = logApi;

    }

    private void setupUserAuthentication() {

        Jwt jwt = Jwt.withTokenValue("automated-task")
            .header("alg", "RS256")
            .claim("emails", List.of(automatedTaskConfigurationProperties.getSystemUserEmail()))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

    }

    @Override
    public void run() {
        executionId = ThreadLocal.withInitial(UUID::randomUUID);
        preRunTask();
        try {

            Optional<AutomatedTaskEntity> automatedTaskEntity = automatedTaskRepository.findByTaskName(getTaskName());
            if (automatedTaskEntity.isPresent()) {
                AutomatedTaskEntity automatedTask = automatedTaskEntity.get();
                String dbCronExpression = automatedTask.getCronExpression();
                // Check the cron expression hasn't been changed in the database by another instance, if so skip this run
                if (getLastCronExpression().equals(dbCronExpression)) {
                    if (TRUE.equals(automatedTask.getTaskEnabled())) {
                        logApi.taskStarted(executionId.get(), this.getTaskName());
                        lockingTaskExecutor.executeWithLock(new LockedTask(), getLockConfiguration());
                    } else {
                        setAutomatedTaskStatus(AutomatedTaskStatus.SKIPPED);
                        log.warn("Not running task {} now as it has been disabled", getTaskName());
                    }
                } else {
                    setAutomatedTaskStatus(AutomatedTaskStatus.SKIPPED);
                    log.warn("Not running task {} now as cron expression has been changed in the database from '{}' to '{}'",
                             getTaskName(), getLastCronExpression(), dbCronExpression
                    );
                }
            }
        } catch (Exception exception) {
            logApi.taskFailed(executionId.get(), getTaskName());
            setAutomatedTaskStatus(AutomatedTaskStatus.FAILED);
            handleException(exception);
        } finally {
            postRunTask();
        }
    }

    @Override
    public AutomatedTaskStatus getAutomatedTaskStatus() {
        return automatedTaskStatus;
    }

    @Override
    public LockConfiguration getLockConfiguration() {
        return new LockConfiguration(
            Instant.now(),
            getTaskName(),
            getLockAtMostFor(),
            getLockAtLeastFor()
        );
    }

    @Override
    public String getLastCronExpression() {
        return lastCronExpression;
    }

    @Override
    public void setLastCronExpression(@NotNull String cronExpression) {
        this.lastCronExpression = cronExpression;
    }

    protected Duration getLockAtMostFor() {
        return Duration.ofSeconds(DEFAULT_LOCK_AT_MOST_SECONDS);
    }

    protected static Duration getLockAtLeastFor() {
        return Duration.ofSeconds(DEFAULT_LOCK_AT_LEAST_SECONDS);
    }

    protected void setAutomatedTaskStatus(AutomatedTaskStatus automatedTaskStatus) {
        log.debug("{} changing status from {} to {}", getTaskName(), this.automatedTaskStatus, automatedTaskStatus);
        this.automatedTaskStatus = automatedTaskStatus;
    }

    protected abstract void runTask();

    protected abstract void handleException(Exception exception);

    private void preRunTask() {
        setupUserAuthentication();
        start = Instant.now();
        log.info("Task : {} started running at: {}", getTaskName(), LocalDateTime.now());
        setAutomatedTaskStatus(AutomatedTaskStatus.IN_PROGRESS);
    }

    private void postRunTask() {
        stopStopwatchAndLogFinished();
        if (getAutomatedTaskStatus().equals(AutomatedTaskStatus.IN_PROGRESS)) {
            setAutomatedTaskStatus(AutomatedTaskStatus.COMPLETED);
            logApi.taskCompleted(executionId.get(), this.getTaskName());
        }

    }

    private void stopStopwatchAndLogFinished() {
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info("Task : {} finished running at: {}", getTaskName(), LocalDateTime.now());
        log.debug("Task : {} time elapsed: {} ms", getTaskName(), timeElapsed);
    }

    class LockedTask implements Runnable {
        @Override
        public void run() {
            try {
                LockAssert.assertLocked();
                runTask();
            } catch (Exception exception) {
                lockedException(exception);
            }
        }

        private void lockedException(Exception exception) {
            setAutomatedTaskStatus(AutomatedTaskStatus.LOCK_FAILED);
            if (exception instanceof IllegalStateException) {
                log.error("Unable to lock task {}", exception.getMessage());
            } else {
                handleException(exception);
            }
        }
    }
}
