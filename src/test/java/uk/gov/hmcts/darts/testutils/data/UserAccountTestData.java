package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Random;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class UserAccountTestData {

    private static final Random RANDOM = new Random();

    public static UserAccountEntity minimalUserAccount() {
        var userAccount = new UserAccountEntity();
        userAccount.setActive(true);
        userAccount.setIsSystemUser(false);
        userAccount.setUserName("some-user-name-" + RANDOM.nextInt(1000, 9999));
        userAccount.setUserFullName("some-user-full-name");
        return userAccount;
    }

}
