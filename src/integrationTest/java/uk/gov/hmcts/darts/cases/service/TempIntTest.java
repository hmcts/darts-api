package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.service.impl.TempService;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TempIntTest extends IntegrationBase {

    @Autowired
    private TempService tempService;

    @BeforeEach
    void setUp() {
        CourthouseEntity glasgowCourthouse = dartsDatabase.createCourthouseUnlessExists("glasgow");
        dartsDatabase.createCourthouseUnlessExists("swansea");

        // Grab any group and set our relation through it
        SecurityGroupEntity securityGroupEntity = dartsDatabase.getSecurityGroupRepository().findAll().get(0);
        securityGroupEntity.setCourthouseEntities(Collections.singleton(glasgowCourthouse));
        dartsDatabase.getSecurityGroupRepository().save(securityGroupEntity);

        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUsername(UUID.randomUUID().toString());
        userAccountEntity.setEmailAddress("glasgow-user@example.com");
        userAccountEntity.setSecurityGroupEntities(Collections.singleton(securityGroupEntity));
        dartsDatabase.getUserAccountRepository().save(userAccountEntity);
    }

    @Test
    void shouldReturnSingleCourthouse_whenUserIsAuthorisedForSingleCourthouse() {
        List<CourthouseEntity> authorisedCourthouses = tempService.getAuthorisedCourthouses("glasgow-user@example.com");

        assertEquals(1, authorisedCourthouses.size());
        assertEquals("glasgow", authorisedCourthouses.get(0).getCourthouseName());
    }

}
