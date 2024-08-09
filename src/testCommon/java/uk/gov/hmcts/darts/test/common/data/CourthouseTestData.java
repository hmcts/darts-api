package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CourthouseTestData {

    public static CourthouseEntity someMinimalCourthouse() {
        var postfix = random(10, false, true);
        var courtHouse = new CourthouseEntity();
        courtHouse.setCourthouseName("some-courthouse-" + postfix);
        courtHouse.setDisplayName("some-courthouse" + postfix);

        var defaultUser = minimalUserAccount();
        courtHouse.setLastModifiedBy(defaultUser);
        courtHouse.setCreatedBy(defaultUser);
        return courtHouse;
    }

    public static CourthouseEntity createCourthouseWithName(String name) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(name);
        return courthouse;
    }

    public static CourthouseEntity createCourthouseWithNameAndCode(String name, Integer code) {
        var courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(name);
        courthouse.setCode(code);
        return courthouse;
    }
}
