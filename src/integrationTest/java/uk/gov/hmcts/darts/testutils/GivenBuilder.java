package uk.gov.hmcts.darts.testutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.component.DartsJwt;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.createGroupForRole;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@Component
public class GivenBuilder {

    @Autowired
    private DartsDatabaseStub dartsDatabase;

    public UserAccountEntity anAuthenticatedUserWithRoles(CourthouseEntity courthouse, SecurityRoleEnum... roles) {
        return anAuthenticatedUserWithRoles(Set.of(courthouse), roles);
    }

    public UserAccountEntity anAuthenticatedUserWithRoles(Set<CourthouseEntity> courthouses, SecurityRoleEnum... roles) {
        UUID userId = UUID.randomUUID();
        var userEmail = userId + "@global.com";


        var user = minimalUserAccount();
        user.setEmailAddress(userEmail);
        user = dartsDatabase.save(user);

        for (SecurityRoleEnum role : roles) {
            SecurityRoleEntity securityRole = dartsDatabase.getSecurityRoleRepository().findById(role.getId()).orElseThrow();
            String groupName = "TEST_GROUP_WITH_ROLE_" + role.name();
            Optional<SecurityGroupEntity> securityGroupOpt = dartsDatabase.getSecurityGroupRepository().findByGroupNameIgnoreCase(groupName);
            SecurityGroupEntity securityGroupEntity;
            if (securityGroupOpt.isEmpty()) {
                securityGroupEntity = SecurityGroupTestData.minimalSecurityGroup(0);
                securityGroupEntity.setGroupName(groupName);
                securityGroupEntity.setSecurityRoleEntity(securityRole);
                securityGroupEntity.getCourthouseEntities().addAll(courthouses);
                dartsDatabase.save(securityGroupEntity);
            } else {
                securityGroupEntity = securityGroupOpt.get();
                securityGroupEntity.getCourthouseEntities().addAll(courthouses);
            }
            dartsDatabase.addUserToGroup(user, securityGroupEntity);
        }
        anAuthenticatedUserFor(user);
        return dartsDatabase.getDartsPersistence().refresh(user);
    }


    public UserAccountEntity anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum role) {
        var userEmail = role.name() + "@global.com";

        var securityGroup = createGroupForRole(role);
        securityGroup.setGlobalAccess(true);

        var user = minimalUserAccount();
        user.setEmailAddress(userEmail);
        user = dartsDatabase.save(user);

        dartsDatabase.addUserToGroup(user, securityGroup);
        anAuthenticatedUserFor(user);

        return dartsDatabase.getDartsPersistence().refresh(user);
    }

    public UserAccountEntity anAuthenticatedUserAuthorizedForCourthouse(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var userEmail = role.name() + "@" + courthouse.getCourthouseName() + ".com";

        var securityGroup = buildGroupForRoleAndCourthouse(JUDICIARY, courthouse);

        var judge = minimalUserAccount();
        judge.setEmailAddress(userEmail);
        judge = dartsDatabase.save(judge);
        dartsDatabase.addUserToGroup(judge, securityGroup);

        anAuthenticatedUserFor(judge);
        return dartsDatabase.getDartsPersistence().refresh(judge);
    }

    public static UserAccountEntity anAuthenticatedUserFor(String email, UserAccountRepository userAccountRepository) {
        return anAuthenticatedUserFor(userAccountRepository.findFirstByEmailAddressIgnoreCase(email).orElseThrow());
    }

    public static UserAccountEntity anAuthenticatedUserFor(UserAccountEntity userAccountEntity) {
        DartsJwt jwt = new DartsJwt(
            Jwt.withTokenValue("some-token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("emails", List.of(userAccountEntity.getEmailAddress()))
                .build(),
            userAccountEntity.getId());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        return userAccountEntity;
    }
}
