# DMP-5207 build performance and stability changes

This file records the changes made while investigating the longer DARTS API build and the stability of integration tests running with three Gradle forks. Add a new row when a repeated build exposes a failure and a working fix is retained.

| Change | Description and reason |
| --- | --- |
| [60-run integration stability check](build.gradle#L220-L295) | The `integration` task now runs the full integration suite 60 times, sequentially. Each run uses three test JVM forks, has separate test reports, is forced to execute rather than being marked up-to-date, and the build fails after all runs if any result contains a failure or error. This lets Jenkins run the stability check without changing the Jenkins pipeline. |
| [Unified integration task](build.gradle#L292-L295) | The earlier H2/PostgreSQL split was removed. The final approach runs the complete integration suite from one `integration` task, rather than creating separate H2 and PostgreSQL tasks or Jenkins stages. |
| [Three parallel test forks](build.gradle#L232-L242) | Each repeated integration run uses `maxParallelForks = 3` to assess whether the suite remains stable when three test JVMs run concurrently. |
| [OpenFeign test connection lifetime](src/integrationTest/resources/application-intTest.yaml#L1-L7) | The integration-test profile expires HTTP connections after 1 ms. WireMock can be recreated while a cached HTTP connection remains open; expiring the connection prevents a later test from reusing a stale socket and receiving `NoHttpResponseException`. This is test-only and does not change production clients. |
| [Spring test-context cache limit](src/integrationTest/resources/spring.properties#L1) | The Spring test context cache is limited to one context per test JVM. This was retained to limit memory use after earlier out-of-memory issues; it trades lower memory consumption for more context creation. |
| [WireMock async-work barrier](src/integrationTest/java/uk/gov/hmcts/darts/testutils/IntegrationBaseWithWiremock.java#L38-L68) | Before a WireMock-backed integration test clears and configures WireMock, the test base waits for the shared async executor to have no active or queued work. This prevents a DAR notification started by the previous test from arriving after the next test has reset WireMock, which otherwise causes incorrect request counts or unmatched requests. |
| [Unit-test log isolation](src/test/java/uk/gov/hmcts/darts/audio/service/impl/AudioUploadServiceImplTest.java#L645-L661) | Removed a successful-path assertion that the shared JVM log did not contain `Failed to delete blob`. Unit tests run concurrently and `CapturedOutput` includes logs from other tests, so that negative assertion could fail even when this test's mocked delete succeeded. The exception-path test still verifies its expected log output. |
| [Media repository result assertion](src/integrationTest/java/uk/gov/hmcts/darts/common/repository/MediaRepositoryIntTest.java#L137-L153) | The repository query has no `ORDER BY` clause, so PostgreSQL is not required to return matching media in ID order. The test now checks that the expected IDs are present, rather than assuming an ordering that the query does not guarantee. |

## Recording future findings

For each retained fix, add a row with:

1. A link to the changed production or test file.
2. The failing test or symptom it addresses.
3. Why the prior behaviour was flaky or slow.
4. What the change does, and whether it affects production or tests only.
