package uk.gov.hmcts.darts.testutils;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class UserAccountTestData {

    public static UserAccountEntity minimalUserAccount() {
        var userAccount = new UserAccountEntity();
        userAccount.setActive(true);
        userAccount.setIsSystemUser(false);
        userAccount.setUserFullName("some-user-full-name");
        return userAccount;
    }

}
