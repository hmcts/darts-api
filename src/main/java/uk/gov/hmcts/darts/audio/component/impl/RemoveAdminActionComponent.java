package uk.gov.hmcts.darts.audio.component.impl;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveAdminActionComponent {

    private final AuditApi auditApi;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;

    private final MediaRepository mediaRepository;
    private final ObjectAdminActionRepository adminActionRepository;
    private final ObjectHiddenReasonRepository hiddenReasonRepository;

    public static final String AUDIT_TEMPLATE = "Media id: %d, Ticket ref: %d, Comments: %s";

    @Transactional
    public boolean removeAdminActionFromAllVersions(@NonNull MediaEntity targetedMedia) {
        log.debug("Attempting to remove all admin actions from all media versions with chronicle id {}",
                  targetedMedia.getChronicleId());

        List<MediaEntity> allMediaVersions = mediaRepository.findAllByChronicleId(targetedMedia.getChronicleId());


        for (MediaEntity media : allMediaVersions) {
            auditApi.record(AuditActivity.UNHIDE_AUDIO, AUDIT_TEMPLATE.formatted(media.getId(), 0, ""));
            media.setHidden(false);
        }
        mediaRepository.saveAllAndFlush(allMediaVersions);

        adminActionRepository.deleteObjectAdminActionEntitiesByMedias(allMediaVersions);

        for (ObjectAdminActionEntity objectAdminActionEntity : objectAdminActionRepository.findByMedia_Id(mediaId)) {
            objectAdminActionRepository.deleteById(objectAdminActionEntity.getId());
        }

    }

    private String buildUnhideAudioAdditionalDataString(ObjectAdminActionEntity objectAdminActionEntity) {
        return "Ticket reference: " + objectAdminActionEntity.getTicketReference() + ", Comments: " + objectAdminActionEntity.getComments();
    }

}
