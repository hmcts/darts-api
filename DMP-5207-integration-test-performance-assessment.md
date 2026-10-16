# DMP-5207 integration-test performance assessment

## Purpose

This assessment covers the DARTS API Gradle `integration` task, its test infrastructure, observed timings, and practical
options for reducing elapsed time. Recommendations are ordered from least to most invasive. A faster suite is not useful
if it becomes flaky, so each recommendation includes the main risk and how to validate it.

## Scope and evidence

The current [`integration` task](build.gradle#L225-L231) executes the complete integration-test source set with three
Gradle test JVM forks. Every Gradle `Test` task currently has a maximum heap of 2 GB
([configuration](build.gradle#L202-L216)).

The following local reports were retained from the DMP-5207 repeated-run investigation on 14 October 2026. They provide
a useful controlled baseline because each run exercised the same suite and completed successfully. They are not Jenkins
measurements and should not be compared directly with an end-to-end pipeline duration.

## Timings observed so far

The reported Jenkins full-pipeline duration has increased from approximately 32–33 minutes to approximately 45–47
minutes. Platform Operations reviewed the retained Jenkins history and found no platform issue; their longest visible
stage was Gradle integration. Jenkins retained only 20 builds, so the earlier 32-minute baseline could not be verified.

The Gradle HTML reports from seven consecutive local integration-suite runs recorded the following elapsed durations:

- Run 1: 5m 46s
- Run 2: 5m 58s
- Run 3: 6m 34s
- Run 4: 6m 00s
- Run 5: 5m 55s
- Run 6: 5m 52s
- Run 7: 5m 36s

That gives a median of 5m 55s, an average of approximately 5m 57s, and a 58-second range. Each run contained 2,297
test methods and no failures. The reports therefore show normal local variation, rather than a dramatic local regression.

Previous pipeline observations put the integration stage at roughly 18–20 minutes. The difference from the local
five-to-six-minute baseline is likely to include slower Jenkins compute, container/image availability, CPU and memory
contention, and cold startup. It should be measured on the same Jenkins agent class before attributing it to test code.

## What the suite is doing

There are 354 Java files under `src/integrationTest/java`; not all are test classes. The report contains 2,297 test
methods. Direct subclasses of the H2 base account for 1,733 methods, and direct subclasses of the WireMock H2 base add
72 more. Direct PostgreSQL-base subclasses account for 387 methods. The remaining methods use other test arrangements.

This means H2-backed tests are at least 79% of the suite, while the identified PostgreSQL tests are about 17%. Splitting
only H2 and PostgreSQL would therefore be poorly balanced: the H2 side would remain the long-running side.

The standard H2 base starts a Redis Testcontainers container and a Spring Boot context
([`IntegrationBase`](src/integrationTest/java/uk/gov/hmcts/darts/testutils/IntegrationBase.java#L71-L95)). PostgreSQL
tests start a PostgreSQL Testcontainers container and their own Spring Boot context
([`PostgresIntegrationBase`](src/integrationTest/java/uk/gov/hmcts/darts/testutils/PostgresIntegrationBase.java#L12-L44)).
WireMock-backed tests also reset stubs and wait for any outstanding asynchronous work before every test
([`IntegrationBaseWithWiremock`](src/integrationTest/java/uk/gov/hmcts/darts/testutils/IntegrationBaseWithWiremock.java#L42-L79)).

Every subclass of `TestBase` clears the database, clears the entity-manager cache, and clears application caches before
each test method ([`TestBase`](src/integrationTest/java/uk/gov/hmcts/darts/testutils/TestBase.java#L46-L55)). The database
clear resets sequences, removes audit data, deletes from many repositories in dependency order, and restores predefined
data ([`DartsDatabaseStub`](src/integrationTest/java/uk/gov/hmcts/darts/testutils/stubs/DartsDatabaseStub.java#L249-L358)).
This is strong isolation, but it is deliberately expensive when performed thousands of times.

The slowest test classes in retained local run 6 were `StandardEventHandlerTest` (21.4 seconds),
`TransformedMediaRepositoryTest` (17.9 seconds), `TranscriptionDocumentRepositoryIntTest` (16.4 seconds),
`DarStartHandlerTest` (15.9 seconds), and `StopAndCloseHandlerTest` (15.3 seconds). These are useful first profiling
targets, but are not individually large enough to explain an 18–20-minute Jenkins stage by themselves.

## Recommended changes, from easiest to hardest

### 1. Measure before changing behaviour

Add temporary, aggregate timing around Spring context startup, Redis/PostgreSQL container startup, `clearDb()`, and
WireMock setup. Report count, total, average, and 95th percentile once per test JVM; do not log one line for every test.
Also capture Gradle worker count, heap, CPU allocation, and available memory in Jenkins.

This is low risk and should be the next step. It turns the current assumption that setup is the main cost into a measured
result. Remove the instrumentation after collecting a representative Jenkins sample, or retain it only behind a Gradle
property.

### 2. Run a controlled Jenkins fork-and-heap experiment

Use the same Jenkins agent class and commit to compare one, two, and three forks. Repeat each configuration at least
three times, recording the Gradle integration elapsed time, peak memory, CPU throttling, and failures. Then compare 1 GB
and 2 GB worker heaps for the best fork count.

This is low implementation effort and moderate operational effort. Three forks are not automatically faster: each starts
and runs a Spring application and test infrastructure, and can compete for CPU, memory, Docker, and database resources.
The current 2 GB setting can allow three workers to grow toward 6 GB before Gradle, containers, and operating-system
overhead. It should be retained only if measurement shows lower GC time or faster elapsed time on Jenkins.

### 3. Enable and verify useful Gradle caching

`org.gradle.caching=true` can avoid work such as compilation and code generation when an agent can reuse a local cache.
It will not make a test task that must execute materially faster. Check whether Jenkins agents are ephemeral and whether a
remote Gradle build cache is available; if not, this setting is unlikely to explain a large pipeline improvement.

This is low risk to test correctness, but a remote cache needs Platform Operations ownership and cache-key/security
controls. Treat it as a full-build optimisation, not an integration-test optimisation.

### 4. Profile and simplify the common database reset

Measure `clearDb()` separately for H2 and PostgreSQL. The method performs sequence resets, audit deletion, many ordered
repository deletes, predefined-data restoration, and a second audit deletion for every test. It also contains repeated
repository delete calls; each should be reviewed with its foreign-key reason before removal.

After measurement, remove safely redundant work and replace per-repository operations with database-native bulk cleanup
only where the H2 and PostgreSQL semantics are proven equivalent. This is medium effort and medium-to-high risk: an
incorrect reset leaks data between tests and creates order-dependent failures. Each change needs repeated parallel-suite
validation, not merely a single green run.

### 5. Recover safe Spring context reuse

The retained configuration limits the Spring test-context cache to one context per test JVM to control memory use. Measure
context-cache hits/misses and memory, then trial a cache size of two or three on a correctly sized Jenkins agent. Also
standardise test configuration where possible so classes share a context rather than creating variations through custom
`@SpringBootTest` properties.

This is medium effort and risk. A larger cache can improve startup time but previously increased memory pressure. It must
be tested together with fork count and heap size; increasing all three simultaneously would make results inconclusive.

### 6. Reduce explicit waiting in the slow asynchronous tests

Profile the five slowest classes, especially the DAR/event-handler tests. Replace fixed or polling waits only when there
is a precise completion signal, such as a future, latch, or recorded event. Keep the existing WireMock asynchronous-work
barrier: it protects test isolation and was added to prevent notifications from a previous test arriving after WireMock
has been reset.

This is medium effort and medium risk. Reducing timeouts without a deterministic completion signal merely turns slow
tests into flaky tests. The aim is less waiting after work has completed, not shorter failure windows.

### 7. Use deliberately balanced Jenkins shards, not an H2/PostgreSQL split

If the fork experiment shows that a single Jenkins executor is the limit, create two or three Gradle tasks containing
measured, balanced groups of test classes and run them in separate Jenkins executors. Each shard needs independent
database/container/WireMock resources and separate test reports. The groups should be based on observed duration, rather
than database type: H2 alone is at least 79% of the suite.

This is high effort and high stability risk. The earlier H2/PostgreSQL split would not balance the work, and increasing
forks has already exposed shared-state and asynchronous timing failures. Implement only after the suite has stable,
repeatable isolation and after Jenkins capacity is confirmed.

### 8. Reclassify or redesign the most expensive tests

Over time, move tests that do not need a full Spring application, database, Redis, and WireMock stack into focused unit
or Spring-slice tests. Keep a smaller number of full workflow tests for each critical integration path. Transactional
rollback can sometimes replace a full database clear for purely database-local tests, but it cannot safely cover HTTP,
asynchronous, scheduled, or separately transacted work without redesign.

This offers the largest possible saving but is the hardest and highest-risk option. It changes the testing strategy and
can reduce integration coverage if done indiscriminately. Apply incrementally, retaining representative end-to-end tests
and proving equivalence for each migrated class.

## Changes not recommended as a first response

Do not raise `maxParallelForks` above three without Jenkins measurements. More workers can slow the job when CPU or
memory is saturated, and can reintroduce shared-resource failures.

Do not restore repeated integration runs to the normal pipeline. Repetition is valuable for a temporary stability study,
but multiplies normal build time and obscures the performance of one suite execution.

Do not split H2 and PostgreSQL solely to obtain concurrency. The split is substantially unbalanced and needs Jenkins
pipeline work and verified resource isolation before it can provide a reliable gain.

## Proposed decision sequence

1. Add aggregate setup/cleanup timing and collect three comparable Jenkins builds.
2. Run the one/two/three-fork and 1 GB/2 GB matrix on the same Jenkins agent type.
3. Select the fastest stable configuration, based on median duration rather than one favourable run.
4. Optimise the measured dominant cost, starting with database cleanup or context creation only if the data supports it.
5. Consider executor-level balanced sharding only if a stable single-task configuration still cannot meet the target.

## Success criteria

A change should be accepted only when it reduces the median Jenkins `gradle integration` elapsed time across at least three
runs, has no test failures, does not increase agent resource pressure beyond its allocation, and preserves the required
H2, PostgreSQL, Redis, WireMock, and asynchronous behaviour coverage.
