package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import java.util.ArrayList;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class CourtroomTestData {

    private CourtroomTestData() {

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
        courtroom.setCreatedBy(minimalUserAccount());
        return courtroom;
    }

    public static CourtroomEntity createCourtRoomWithNameAtCourthouse(CourthouseEntity courthouse, String name) {
        var courtroom = someMinimalCourtRoom();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }
}