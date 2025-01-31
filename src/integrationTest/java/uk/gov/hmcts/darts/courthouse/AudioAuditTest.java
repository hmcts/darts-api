package uk.gov.hmcts.darts.courthouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EntityGraphPersistence;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaRequestTestData;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class AudioAuditTest extends IntegrationBase {

    @Autowired
    private EntityGraphPersistence entityGraphPersistence;

    @Autowired
    private MediaRequestService mediaRequestService;

    @Autowired
    private GivenBuilder given;

    @Test
    void performsStandardAndAdvancedAuditsWhenAudioOwnershipIsChanged() {
        var activeUser = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        MediaRequestEntity mediaRequest = getMediaRequestTestData().someMinimalRequestData();
        dartsDatabase.save(mediaRequest.getHearing());
        dartsDatabase.save(mediaRequest);
        UserAccountEntity newOwner = dartsDatabase.save(minimalUserAccount());

        mediaRequestService.patchMediaRequest(
            mediaRequest.getId(),
            new MediaPatchRequest().ownerId(newOwner.getId()));

        var changeAudioOwnership = findAuditActivity("Changing Audio Ownership", dartsDatabase.findAudits());
        assertThat(changeAudioOwnership.getUser().getId()).isEqualTo(activeUser.getId());

        var mediaRequestRevisions = dartsDatabase.findMediaRequestRevisionsFor(mediaRequest.getId());
        assertThat(mediaRequestRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
    }

    @Test
    void performsStandardAuditWhenAudioIsChangedToHidden() {
        var activeUser = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var media = PersistableFactory.getMediaTestData().someMinimalMedia();
        media.setHidden(false);
        dartsDatabase.save(media.getCourtroom().getCourthouse());
        dartsDatabase.save(media.getCourtroom());
        dartsDatabase.save(media);

        mediaRequestService.adminHideOrShowMediaById(
            media.getId(),
            new MediaHideRequest()
                .isHidden(true)
                .adminAction(new AdminActionRequest().reasonId(4)));

        var hidingAudioAuditActivity = findAuditActivity("Hiding Audio", dartsDatabase.findAudits());
        assertThat(hidingAudioAuditActivity.getUser().getId()).isEqualTo(activeUser.getId());
    }

    private AuditEntity findAuditActivity(String activity, List<AuditEntity> audits) {
        return audits.stream()
            .filter(audit -> activity.equals(audit.getAuditActivity().getName()))
            .findFirst().orElseThrow();
    }

}