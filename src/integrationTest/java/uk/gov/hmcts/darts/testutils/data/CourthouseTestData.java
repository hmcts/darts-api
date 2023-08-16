package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomAtCourthouse;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CourthouseTestData {

    public static CourthouseEntity someMinimalCourthouse() {
        var courtHouse = new CourthouseEntity();
        courtHouse.setCourthouseName("some-courthouse");
        return courtHouse;
    }

    public static CourthouseEntity createCourthouse(String name) {
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        return courthouse;
    }

    public static CourthouseEntity createCourthouse(String name, Integer code) {
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setCode(code);
        return courthouse;
    }

    public static CourthouseEntity createCourthouseWithRoom(String courthouseName, String someRoomName) {
        var courthouse = createCourthouse(courthouseName);
        var courtroom = createCourtRoomAtCourthouse(courthouse);
        courtroom.setName(someRoomName);
        courthouse.addCourtRoom(courtroom);
        return courthouse;
    }
}
