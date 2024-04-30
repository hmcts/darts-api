package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import java.util.Random;

import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CourtroomTestData {

    private static final Random RANDOM = new Random();

    public static CourtroomEntity someMinimalCourtRoom() {
        int postfix = RANDOM.nextInt(100000, 999999);
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
