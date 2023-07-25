package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class CourtroomTestData {

    public static CourtroomEntity someMinimalCourtRoom() {
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(createCourthouse("some-courthouse"));
        courtroom.setName("room_a");
        return courtroom;
    }

    public static CourtroomEntity createCourtRoomAtCourthouse(CourthouseEntity courthouse) {
        var courtroom = someMinimalCourtRoom();
        courtroom.setCourthouse(courthouse);
        return courtroom;
    }

    public static CourtroomEntity createCourtRoomWithNameAtCourthouse(CourthouseEntity courthouse, String name) {
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }
}
