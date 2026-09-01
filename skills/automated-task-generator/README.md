# Automated Task Generator

`automated-task-generator.md` is a reusable Codex guide for adding new DARTS automated tasks consistently.
It is based on the pattern introduced by `InboundAudioFailureCorrectionAutomatedTask` in PR `hmcts/darts-api#3362`.

## What It Creates

Use the generator when a new scheduled or manually runnable task needs the standard DARTS automated-task wiring:

- a Flyway migration in `src/main/resources/db/migration/common`
- an `automated_task` row
- a matching system user in `user_account`
- an `AutomatedTaskName` enum entry
- a Spring `@ConfigurationProperties` task config
- a `darts.automated.task.<task-name>` block in `application.yaml`
- a lockable task runner in `uk.gov.hmcts.darts.task.runner.impl`
- a domain service interface and implementation stub for the business logic

## Required Inputs

Provide these values to the skill:

```text
Task name: <PascalCase task name, e.g. InboundAudioFailureCorrection>
Description: <automated_task.task_description>
User ID: <negative usr_id for the task system user>

What should the automated task do?
<Describe the data it reads, the action it performs, dependencies it may use, and the owning domain.>
```

The task-behaviour description is important. The generator uses it to decide where the service stub should live, for
example `audio`, `datamanagement`, `arm`, `retention`, `transcriptions`, `dailylist`, or another existing domain package.

Optional values include cron expression, batch size, enabled state, cron editability, lock durations, whether the task is
manual-runnable, and whether it needs async task configuration.

## How To Use

Open `automated-task-generator.md`, fill in the request template, and ask Codex to generate the task from it. A good prompt
looks like this:

```text
Use automated-task-generator.md to create a new automated task.

Task name: ExampleCleanup
Description: Cleans up example records after successful export
User ID: -54

What should the automated task do?
Find exported example records older than 30 days, mark them as cleaned up, and record the system user as the modifier.
This belongs in the data management domain and will likely use ExampleRecordRepository.

Batch size: 1000
Task enabled: false
Cron expression: 0 15 2 * * *
```

## Expected Review Points

After generation, check:

- the Flyway migration version is unique
- `system-user-email` exactly matches the inserted system user email
- `AutomatedTaskName` exactly matches `automated_task.task_name`
- the runner only handles scheduling, locking, and delegation
- business logic lives in the selected domain service
- focused unit tests exist once the service contains real branching, validation, mapping, repository selection, error handling, or batch logic

## Local Validation

Run focused tests for the touched service or runner first. Before PR handoff, run:

```bash
./gradlew check
```

If the change touches integration behaviour, also run the relevant focused `./gradlew integration --tests '<Pattern>'`.
