package uk.gov.hmcts.darts.audit.service;

import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audit.model.RevisionInfo;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.service.UserManagementService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditorRevisionListenerIntTest extends PostgresIntegrationBase {

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


        UserAccountEntity modifiedUser = dartsDatabase.getUserAccountStub().createSuperAdminUser();
        GivenBuilder.anAuthenticatedUserFor(modifiedUser);
        UserPatch userPatch = new UserPatch();
        userPatch.setFullName("Some Other Name");
        userManagementService.modifyUser(createdUser.getId(), userPatch);

        AuditReader reader = AuditReaderFactory.get(sessionFactory.createEntityManager());


        List<Object[]> audits = reader.createQuery()
            .forRevisionsOfEntity(UserAccountEntity.class, false, false)
            .add(AuditEntity.property("id").eq(createdUser.getId()))
            .getResultList();

        assertThat(audits).hasSize(2);
        Object[] createAudit = audits.get(0);
        Object[] editAudit = audits.get(1);

        RevisionInfo createAuditRevisionInfo = (RevisionInfo) createAudit[1];
        assertThat(createAuditRevisionInfo.getAuditUser()).isEqualTo(currentUser.getId());

        RevisionInfo editAuditRevisionInfo = (RevisionInfo) editAudit[1];
        assertThat(editAuditRevisionInfo.getAuditUser()).isEqualTo(modifiedUser.getId());

    }
}
