package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestCourtroomEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;

public final class CourtroomTestData implements Persistable<TestCourtroomEntity.TestCourtroomEntityBuilderRetrieve,
    CourtroomEntity,
    TestCourtroomEntity.TestCourtroomEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Override
    public CourtroomEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestCourtroomEntity.TestCourtroomEntityBuilderRetrieve someMinimalBuilderHolder() {
        var builderRetrieve = new TestCourtroomEntity.TestCourtroomEntityBuilderRetrieve();

        builderRetrieve.getBuilder()
            .courthouse(PersistableFactory.getCourthouseTestData().someMinimal())
            .name("SOME COURTROOM (%s)".formatted(UUID.randomUUID().toString()))
            .createdDateTime(NOW)
            .createdById(0);

        return builderRetrieve;
    }

    @Override
    public TestCourtroomEntity.TestCourtroomEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    public static CourtroomEntity someMinimalCourtRoom() {
        var postfix = random(10, false, true);
        var courtroom = new CourtroomEntity();
        var courthouse = createCourthouseWithName("some-courthouse-" + postfix);
        var courtrooms = new ArrayList<CourtroomEntity>();
        courtrooms.add(courtroom);
        courthouse.setCourtrooms(courtrooms);
        courtroom.setCourthouse(courthouse);
        courtroom.setName("room_a-" + postfix);
        courtroom.setCreatedById(0);
        return courtroom;
    }

    public static CourtroomEntity createCourtRoomWithNameAtCourthouse(CourthouseEntity courthouse, String name) {
        var courtroom = someMinimalCourtRoom();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }

}