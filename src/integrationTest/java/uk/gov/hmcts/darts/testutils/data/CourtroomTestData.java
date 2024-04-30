package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import java.util.Random;

import static org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CourtroomTestData {

    public static CourtroomEntity someMinimalCourtRoom() {
        var postfix = random(10);
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(createCourthouse("some-courthouse-" + postfix));
        courtroom.setName("room_a-" + postfix);
        return courtroom;
    }

    public static CourtroomEntity createCourtRoomWithNameAtCourthouse(CourthouseEntity courthouse, String name) {
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }
}
