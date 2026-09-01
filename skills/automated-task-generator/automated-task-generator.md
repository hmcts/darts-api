# Create a DARTS Automated Task

Use this skill when adding a new automated task to `darts-api`. It captures the current task pattern used by
`InboundAudioFailureCorrectionAutomatedTask`: a Flyway row in `automated_task`, a matching system user, a Spring
configuration class, a runner under `uk.gov.hmcts.darts.task.runner.impl`, and a domain service stub where the task
business logic belongs.

## Required Request Template

Ask the user for these values before generating files:

```text
Task name: <PascalCase task name used in automated_task.task_name, e.g. InboundAudioFailureCorrection>
Description: <human-readable automated_task.task_description>
User ID: <negative usr_id for the system user, e.g. -53>

What should the automated task do?
<Describe the data it reads, the business action it performs, the repositories/services/APIs it is likely to use,
and the domain area it belongs to, e.g. audio, ARM, data management, retention, transcription, daily list.>

Optional:
Cron expression: <Spring cron expression, default to a safe disabled/off-hours value if unknown>
Batch size: <integer, default 1000>
Task enabled: <true|false, default false unless the user explicitly wants it active on deploy>
Cron editable: <true|false, default true>
Lock at least for: <ISO-8601 duration, default PT1M>
Lock at most for: <ISO-8601 duration, default PT45M>
Manual runnable: <true|false, default true>
Async task: <true|false, default false>
```

The "What should the automated task do?" answer controls where to place the service stub. Inspect existing packages
before choosing:

- Audio or media repair/linking/deletion: `src/main/java/uk/gov/hmcts/darts/audio/service`.
- ARM, RPO, DETS, object storage, or response-file work: prefer the existing `arm`, `dets`, or `datamanagement`
  package that already owns the closest service/repository flow.
- Retention, case expiry, transcription, daily list, annotation, or admin work: place the service in that domain package.
- If no clear domain exists, create a narrow service interface under the closest existing domain package rather than under
  `task`; keep `task.runner.impl` limited to scheduling, locking, batch-size lookup, and delegation.

## Generation Steps

1. Derive naming:
   - `TaskName`: exact PascalCase value for `automated_task.task_name`.
   - `task-name`: kebab-case for `darts.automated.task.<task-name>`.
   - `TASK_NAME_TASK_NAME`: enum constant in `AutomatedTaskName`.
   - `TaskNameAutomatedTask`: runner class.
   - `TaskNameAutomatedTaskConfig`: config class.
   - `TaskNameService` and `TaskNameServiceImpl`: domain service stub names unless the domain already has a better API.
   - `system_TaskNameAutomatedTask@hmcts.net`: default system user email, unless the user provides a specific value.

2. Add a Flyway migration under `src/main/resources/db/migration/common` using the next available version:

```sql
INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), '<TaskName>', '<Description>', '<Cron expression>', <cron_editable>, <batch_size>,
        current_timestamp, 0, current_timestamp, 0, <task_enabled>);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by, is_system_user,
                          is_active, user_full_name)
VALUES (<user_id>, 'system_<TaskName>AutomatedTask', 'system_<TaskName>AutomatedTask@hmcts.net',
        'system_<TaskName>AutomatedTask',
        current_timestamp, current_timestamp, 0, 0, true, true, 'system_<TaskName>AutomatedTask');
```

3. Add the enum entry in `src/main/java/uk/gov/hmcts/darts/task/api/AutomatedTaskName.java`:

```java
<TASK_NAME>_TASK_NAME("<TaskName>");
```

Keep enum punctuation valid: add a comma to the previous final entry, and keep the semicolon on the final entry.

4. Add config in `src/main/java/uk/gov/hmcts/darts/task/config/<TaskName>AutomatedTaskConfig.java`:

```java
package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("darts.automated.task.<task-name>")
@Getter
@Setter
@Configuration
public class <TaskName>AutomatedTaskConfig extends AbstractAutomatedTaskConfig {
}
```

Use `AbstractAsyncAutomatedTaskConfig` only when the task genuinely needs the repo's async task properties.

5. Add configuration to `src/main/resources/application.yaml` under `darts.automated.task`:

```yaml
      <task-name>:
        system-user-email: system_<TaskName>AutomatedTask@hmcts.net
        lock:
          at-least-for: PT1M
          at-most-for: PT45M
```

6. Add the domain service interface in the package selected from the user's task-behaviour brief:

```java
package uk.gov.hmcts.darts.<domain>.service;

@FunctionalInterface
public interface <TaskName>Service {

    void process(int batchSize);
}
```

7. Add a stub implementation in the matching `impl` package:

```java
package uk.gov.hmcts.darts.<domain>.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.<domain>.service.<TaskName>Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class <TaskName>ServiceImpl implements <TaskName>Service {

    @Override
    public void process(int batchSize) {
        log.info("Running <TaskName> with batch size {}", batchSize);
        // TODO: Implement task behaviour.
    }
}
```

Replace `process` with a domain-specific verb when the user's brief makes one obvious.

8. Add the runner in `src/main/java/uk/gov/hmcts/darts/task/runner/impl/<TaskName>AutomatedTask.java`:

```java
package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.<domain>.service.<TaskName>Service;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.<TaskName>AutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Slf4j
@Component
public class <TaskName>AutomatedTask extends AbstractLockableAutomatedTask<<TaskName>AutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final <TaskName>Service <taskName>Service;

    public <TaskName>AutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                   <TaskName>AutomatedTaskConfig automatedTaskConfig,
                                   LogApi logApi,
                                   LockService lockService,
                                   <TaskName>Service <taskName>Service) {
        super(automatedTaskRepository, automatedTaskConfig, logApi, lockService);
        this.<taskName>Service = <taskName>Service;
    }

    @Override
    protected void runTask() {
        <taskName>Service.process(getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.<TASK_NAME>_TASK_NAME;
    }
}
```

Use `AutoloadingManualTask` when the task should be available through the admin manual-run endpoint. Use the narrower
autoloading interface used by comparable tasks if the task should not be manually runnable.

## Tests And Checks

- Add unit tests for the service implementation as soon as there is branching, validation, mapping, repository selection,
  error handling, or batch logic.
- Add or update integration tests when scheduling, task lookup, DB migration data, cron reload, repository queries, or
  task administration behaviour changes.
- Run focused tests first, then `./gradlew check` before PR handoff when practical.
- Confirm the new migration version is unique and in `common`, not `reference/manual-data-fixes`.
- Confirm `system-user-email` exactly matches the inserted system user's email.
- Confirm `AutomatedTaskName.<TASK_NAME>_TASK_NAME.getTaskName()` exactly matches `automated_task.task_name`.
