package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestCourthouseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

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

        UserAccountEntity someUser = PersistableFactory.getUserAccountTestData().someMinimal();

        builderRetrieve.getBuilder()
            .courthouseName("SOME COURTHOUSE (%s)".formatted(UUID.randomUUID().toString()))
            .createdDateTime(NOW)
            .lastModifiedDateTime(NOW)
            .createdBy(someUser)
            .lastModifiedBy(someUser)
            .displayName("Some Courthouse");

        return builderRetrieve;
    }

    @Override
    public TestCourthouseEntity.TestCourthouseEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    // Deprecated in favour of the methods provided by the Persistable interface
    @Deprecated
    public static CourthouseEntity someMinimalCourthouse() {
        var postfix = random(10, false, true);
        var courtHouse = new CourthouseEntity();
        courtHouse.setCourthouseName("some-courthouse-" + postfix);
        courtHouse.setDisplayName("SOME-COURTHOUSE" + postfix);

        var defaultUser = minimalUserAccount();
        courtHouse.setLastModifiedBy(defaultUser);
        courtHouse.setCreatedBy(defaultUser);
        return courtHouse;
    }

    // Deprecated, please use the methods provided by the Persistable interface
    @Deprecated
    public static CourthouseEntity createCourthouseWithName(String name) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(name);
        return courthouse;
    }

    // Deprecated, please use the methods provided by the Persistable interface
    @Deprecated
    public static CourthouseEntity createCourthouseWithDifferentNameAndDisplayName(String name, String displayName) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(displayName);
        return courthouse;
    }

}