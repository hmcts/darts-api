package uk.gov.hmcts.darts.courthouse;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.auditing.DartsEnversPostUpdateEventListener;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EntityGraphPersistence;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.event.spi.EventType.POST_UPDATE;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaRequestTestData;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class AudioAuditTest extends IntegrationBase {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityGraphPersistence entityGraphPersistence;

    @Autowired
    private MediaRequestService mediaRequestService;

    @Autowired
    private GivenBuilder given;

    @Test
    void given_applicationContext_when_registeringAuditListeners_then_useDartsUpdateListener() {
        var registry = entityManagerFactory.unwrap(SessionFactoryImpl.class).getServiceRegistry().getService(EventListenerRegistry.class);

        assertThat(registry).isNotNull();
        assertThat(registry.getEventListenerGroup(POST_UPDATE).listeners()).anyMatch(DartsEnversPostUpdateEventListener.class::isInstance);
    }

    @Test
    void performsStandardAndAdvancedAuditsWhenAudioOwnershipIsChanged() {
        var activeUser = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        MediaRequestEntity mediaRequest = getMediaRequestTestData().someMinimalRequestData();
        dartsDatabase.save(mediaRequest.getHearing());
        dartsDatabase.save(mediaRequest.getCurrentOwner());
        dartsDatabase.save(mediaRequest);
        UserAccountEntity newOwner = dartsDatabase.save(minimalUserAccount());

        mediaRequestService.patchMediaRequest(
            mediaRequest.getId(),
            new MediaPatchRequest().ownerId(newOwner.getId()));
        transactionalUtil.executeInTransaction(() -> {
            var changeAudioOwnership = findAuditActivity("Changing Audio Ownership", dartsDatabase.findAudits());
            assertThat(changeAudioOwnership.getUser().getId()).isEqualTo(activeUser.getId());

            var mediaRequestRevisions = dartsDatabase.findMediaRequestRevisionsFor(mediaRequest.getId());
            assertThat(mediaRequestRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        });
    }

    private AuditEntity findAuditActivity(String activity, List<AuditEntity> audits) {
        return audits.stream()
            .filter(audit -> activity.equals(audit.getAuditActivity().getName()))
            .findFirst().orElseThrow();
    }

}
