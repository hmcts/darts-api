package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestCourthouseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.random;

public final class CourthouseTestData implements Persistable<TestCourthouseEntity.TestCourthouseEntityBuilderRetrieve,
    CourthouseEntity,
    TestCourthouseEntity.TestCourthouseEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Override
    public CourthouseEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestCourthouseEntity.TestCourthouseEntityBuilderRetrieve someMinimalBuilderHolder() {
        var builderRetrieve = new TestCourthouseEntity.TestCourthouseEntityBuilderRetrieve();

        builderRetrieve.getBuilder()
            .courthouseName("SOME COURTHOUSE (%s)".formatted(UUID.randomUUID().toString()))
            .createdDateTime(NOW)
            .lastModifiedDateTime(NOW)
            .createdById(0)
            .lastModifiedById(0)
            .displayName("Some Courthouse");

        return builderRetrieve;
    }

    @Override
    public TestCourthouseEntity.TestCourthouseEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    /**
     * Create a "minimal" courthouse entity.
     *
     * @deprecated Tests should be refactored to use the entity creation methods provided by the {@link Persistable} interface.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    public static CourthouseEntity someMinimalCourthouse() {
        var postfix = random(10, false, true);
        var courtHouse = new CourthouseEntity();
        courtHouse.setCourthouseName("some-courthouse-" + postfix);
        courtHouse.setDisplayName("SOME-COURTHOUSE" + postfix);

        courtHouse.setLastModifiedById(0);
        courtHouse.setCreatedById(0);
        return courtHouse;
    }

    /**
     * Create a courthouse with the given name.
     *
     * @deprecated Tests should be refactored to use the entity creation methods provided by the {@link Persistable} interface.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    public static CourthouseEntity createCourthouseWithName(String name) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(name);
        return courthouse;
    }

    /**
     * Create a courthouse with the given name and display name.
     *
     * @deprecated Tests should be refactored to use the entity creation methods provided by the {@link Persistable} interface.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    public static CourthouseEntity createCourthouseWithDifferentNameAndDisplayName(String name, String displayName) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(displayName);
        return courthouse;
    }
}