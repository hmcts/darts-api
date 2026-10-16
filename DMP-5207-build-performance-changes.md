# DMP-5207 build performance and stability changes

This file records the retained changes in the current branch. Add a new section when a repeated build exposes a failure
and a working fix is retained.

## OpenFeign test connection lifetime

[The integration-test profile](src/integrationTest/resources/application-intTest.yaml#L1-L7) expires HTTP connections
after 1 ms. WireMock can be recreated while a cached HTTP connection remains open; expiring the connection prevents a
later test from reusing a stale socket and receiving `NoHttpResponseException`.

This applies to integration tests only and does not change production clients.

## Spring test-context cache limit

[The Spring test-context cache](src/integrationTest/resources/spring.properties#L1) is limited to one context per test
JVM. This limits memory use, at the cost of creating more Spring contexts during the suite.

## WireMock async-work barrier

[The WireMock integration-test base](src/integrationTest/java/uk/gov/hmcts/darts/testutils/IntegrationBaseWithWiremock.java#L38-L68)
waits for the shared async executor to have no active or queued work before it clears and configures WireMock.

This keeps a DAR notification with the test that created it. Without the barrier, a notification from the previous test
could arrive after the next test reset WireMock, causing incorrect request counts or unmatched requests.

## Unit-test log isolation

[The successful audio-upload delete test](src/test/java/uk/gov/hmcts/darts/audio/service/impl/AudioUploadServiceImplTest.java#L645-L661)
no longer asserts that the shared JVM log does not contain `Failed to delete blob`.

Unit tests run concurrently and `CapturedOutput` includes logs from other tests, so this negative assertion could fail
even when the test's mocked delete succeeded. The exception-path test still verifies its expected log output.

## Media repository result assertion

[The media repository integration test](src/integrationTest/java/uk/gov/hmcts/darts/common/repository/MediaRepositoryIntTest.java#L137-L153)
now checks that the expected media IDs are present without assuming their order.

The repository query has no `ORDER BY` clause, so PostgreSQL is not required to return matching rows in ID order.

## Node registration timestamp assertion

[The node registration controller integration test](src/integrationTest/java/uk/gov/hmcts/darts/noderegistration/controller/NodeRegistrationControllerGetNodeRegistrationDevicesTest.java#L50-L105)
now serializes the expected timestamps with the configured application `ObjectMapper`.

The previous assertion manually formatted the expected timestamp and used a narrow workaround for one fractional-second
format. Depending on trailing zeroes, the API could correctly return the same instant with a different string precision.
Using the same serializer as the API removes that formatting-only failure. This is a test-only change.

## Recording future findings

For each retained fix, add a section containing a link to the changed file, the failing test or symptom, the cause, and
whether the change affects production or tests only.
