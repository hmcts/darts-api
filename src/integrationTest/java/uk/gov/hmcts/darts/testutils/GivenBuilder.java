package uk.gov.hmcts.darts.testutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.createGroupForRole;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@Component
public class GivenBuilder {

    @Autowired
    private DartsDatabaseStub dartsDatabase;

    public UserAccountEntity anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum role) {
        var userEmail = role.name() + "@global.com";
        anAuthenticatedUserFor(userEmail);

        var securityGroup = createGroupForRole(role);
        securityGroup.setGlobalAccess(true);

        var user = minimalUserAccount();
        user.setEmailAddress(userEmail);

        dartsDatabase.addUserToGroup(user, securityGroup);

        return user;
    }

    public UserAccountEntity anAuthenticatedUserAuthorizedForCourthouse(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var userEmail = role.name() + "@" + courthouse.getCourthouseName() + ".com";
        anAuthenticatedUserFor(userEmail);

        var securityGroup = buildGroupForRoleAndCourthouse(JUDICIARY, courthouse);

        var judge = minimalUserAccount();
        judge.setEmailAddress(userEmail);

        dartsDatabase.addUserToGroup(judge, securityGroup);

        return judge;
    }

    public static void anAuthenticatedUserFor(String userEmail) {
        Jwt jwt = Jwt.withTokenValue("some-token")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(userEmail))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
