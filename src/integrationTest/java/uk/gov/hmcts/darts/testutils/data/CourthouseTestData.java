package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
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
}
