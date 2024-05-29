package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import static org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils.random;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CourthouseTestData {

    public static CourthouseEntity someMinimalCourthouse() {
        var postfix = random(10);
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
