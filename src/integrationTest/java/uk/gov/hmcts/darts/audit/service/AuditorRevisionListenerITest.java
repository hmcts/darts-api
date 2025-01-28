package uk.gov.hmcts.darts.audit.service;

import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audit.model.RevisionInfo;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.service.UserManagementService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditorRevisionListenerITest extends PostgresIntegrationBase {

    @Autowired
    private UserManagementService userManagementService;
    @Autowired
    private SessionFactory sessionFactory;

    @Test
    @SuppressWarnings("unchecked")
    void auditorRevisionListener_shouldSetAuditUser() {
        UserAccountEntity currentUser = dartsDatabase.createTestUserAccount();
        GivenBuilder.anAuthenticatedUserFor(currentUser);
        User user = new User();
        user.setEmailAddress("someemail@email.com");
        user.setFullName("Some Name");
        user.setDescription("Some Description");

        UserWithId createdUser = userManagementService.createUser(user);


        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getEmailAddress()).isEqualTo("someemail@email.com");
        assertThat(createdUser.getFullName()).isEqualTo("Some Name");
        assertThat(createdUser.getDescription()).isEqualTo("Some Description");

        AuditReader reader = AuditReaderFactory.get(sessionFactory.createEntityManager());

        List<RevisionInfo> audits = reader.createQuery()
            .forRevisionsOfEntity(UserAccountEntity.class, true)
            .getResultList();

        assertThat(audits.get(0).getAuditUser()).isNull();//Created without user loggedin so should be null
        assertThat(audits.get(audits.size() - 1).getAuditUser()).isEqualTo(currentUser.getId());//Updated with user loggedin so should be current user
    }
}
