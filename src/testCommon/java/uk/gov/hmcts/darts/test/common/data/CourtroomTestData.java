package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.ArrayList;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CourtroomTestData {

    public static CourtroomEntity someMinimalCourtRoom() {
        var postfix = random(10, false, true);
        var courtroom = new CourtroomEntity();
        var courthouse = createCourthouse("some-courthouse-" + postfix);
        var courtrooms = new ArrayList<CourtroomEntity>();
        courtrooms.add(courtroom);
        courthouse.setCourtrooms(courtrooms);
        courtroom.setCourthouse(courthouse);
        courtroom.setName("room_a-" + postfix);
        courtroom.setCreatedBy(minimalUserAccount());
        return courtroom;
    }

    public static CourtroomEntity createCourtRoomWithNameAtCourthouse(CourthouseEntity courthouse, String name) {
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        UserAccountEntity defaultUser = UserAccountTestData.minimalUserAccount();
        defaultUser.setId(1000);
        courtroom.setCreatedBy(defaultUser);
        return courtroom;
    }
}
