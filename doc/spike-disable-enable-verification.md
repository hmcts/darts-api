# Disabled automated task deployment verification

Task: `SpikeDisableAndEnableInDeployedEnvironment`. Feature branch:
`codex/spike-disable-enable-prototype`. System user: `-54`.

This find files prototype only logs its execution time; it does not find or change files.
The Flyway migration deploys it disabled with Spring cron `0 */5 * * * *`.

## Environment selection

`Jenkinsfile_CNP` defines the PR deployment pipeline. `Jenkinsfile_parameterized`
provides a sandbox (`sbox`) pipeline, which is a candidate for the second environment.
Confirm the actual PR URL and an available sandbox before deploying; repository
configuration does not establish live availability or a particular PR number.

## Procedure (repeat in each environment)

1. Deploy the feature branch through the appropriate pipeline. Record its build URL,
   commit SHA, environment URL, migration outcome and UTC deployment time.
2. As a super admin, locate the task through `GET /admin/automated-tasks` and
   record its ID. Confirm its detail reports `is_active: false` and the five-minute cron.
3. Observe at least two complete scheduled boundaries after deployment. Verify there
   are no `SpikeDisableAndEnableInDeployedEnvironment executed at` messages.
4. Enable only this task in the selected environment with
   `PATCH /admin/automated-tasks/{id}`, JSON body `{"is_active": true}`.
   Record the response and UTC time. Do not change a shared Helm default or migration.
5. Observe at least two scheduled boundaries. Capture the `executed at` messages and
   verify five-minute spacing. Confirm the other environment remains disabled.
6. Disable using the same endpoint with `{"is_active": false}`. Record response/time.
   Allow any in-flight execution to finish; observe two further complete boundaries
   and confirm no new `executed at` messages. Verify the detail remains disabled.
7. Repeat in the second environment, checking isolation against the first environment.
   Leave both disabled.

Do not use the manual-run endpoint as evidence: manual execution can bypass the
framework's enabled flag. Framework logs such as `potential candidate for execution`
and `finished running` can occur for skipped tasks and are not evidence of execution.
Use the prototype service's exact `executed at` marker across all replicas.

## Evidence

| Environment | Deployment/build | Initially disabled | Enabled and runs | Disabled and stops | Other environment unchanged |
| --- | --- | --- | --- | --- | --- |
| PR (URL pending) | Pending | Pending | Pending | Pending | Pending |
| Sandbox (availability pending) | Pending | Pending | Pending | Pending | Pending |

The integration test checks migrated defaults and the runner's disabled → enabled →
disabled behaviour. It does not prove deployment, live scheduling or environment isolation.

## Local validation (30 October 2026)

- Java 21 `compileJava`, `checkstyleMain` and `pmdMain`: passed in both the original checkout and the isolated PR branch.
- Focused integration test and `check`: blocked before test execution by DNS resolution
  failure for `artifactory.platform.hmcts.net` while resolving test dependencies.
- PR deployment evidence remains pending.
- The configured Kubernetes context is `ss-stg-00-aks`; its API access failed
  with a connection reset/timeout, so no live deployment evidence was collected.
