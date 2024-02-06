package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.util.Random;

/*
 DEPRECATED - Use databaseStubs instead."
 */
@UtilityClass
@Deprecated
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CourthouseTestData {

    private static final Random RANDOM = new Random();

    public static CourthouseEntity someMinimalCourthouse() {
        int postfix = RANDOM.nextInt(1000, 9999);
        var courtHouse = new CourthouseEntity();
        courtHouse.setCourthouseName("some-courthouse-" + postfix);
        courtHouse.setDisplayName("some-courthouse" + postfix);
        return courtHouse;
    }

    public static CourthouseEntity createCourthouse(String name) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(name);
        return courthouse;
    }

    public static CourthouseEntity createCourthouse(String name, Integer code) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(name);
        courthouse.setCode(code);
        return courthouse;
    }
}
