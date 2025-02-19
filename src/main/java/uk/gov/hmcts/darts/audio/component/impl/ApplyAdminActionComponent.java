package uk.gov.hmcts.darts.audio.component.impl;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;

import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.HIDE_AUDIO;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyAdminActionComponent {

    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;
    private final RemoveAdminActionComponent removeAdminActionComponent;

    private final MediaRepository mediaRepository;
    private final ObjectAdminActionRepository adminActionRepository;

    private final AuditApi auditApi;

    private static final String AUDIT_TEMPLATE = "Media id: %d, Ticket ref: %s";

    public record AdminActionProperties(String ticketReference, String comments, ObjectHiddenReasonEntity hiddenReason) {}

    @Transactional
    public List<MediaEntity> applyAdminActionToAllVersions(@NonNull MediaEntity targetedMedia,
                                                           @NonNull AdminActionProperties adminActionProperties) {

        List<MediaEntity> allMediaVersions;
        final String ticketReference = adminActionProperties.ticketReference();
        if (StringUtils.isBlank(targetedMedia.getChronicleId())) {
            log.debug("Attempting to apply admin action with ticket ref {} to non-versioned media with id {}",
                      ticketReference,
                      targetedMedia.getId());
            allMediaVersions = Collections.singletonList(targetedMedia);
        } else {
            log.debug("Attempting to apply admin action with ticket ref {} to all versioned medias with chronicle id {}",
                      ticketReference,
                      targetedMedia.getChronicleId());
            allMediaVersions = mediaRepository.findAllByChronicleId(targetedMedia.getChronicleId());
        }

        return applyAdminActionTo(allMediaVersions, adminActionProperties);
    }

    @Transactional
    public List<MediaEntity> applyAdminActionTo(@NonNull List<MediaEntity> mediaVersions,
                                                @NonNull AdminActionProperties adminActionProperties) {
        // We need to first remove any existing admin actions so that we can link new actions reflecting the details in the incoming adminActionRequest
        removeAdminActionComponent.removeAdminActionFrom(mediaVersions);

        final UserAccountEntity userAccount = userIdentity.getUserAccount();

        final String ticketReference = adminActionProperties.ticketReference();
        for (MediaEntity media : mediaVersions) {

            auditApi.record(HIDE_AUDIO, AUDIT_TEMPLATE.formatted(media.getId(), ticketReference));

            ObjectAdminActionEntity adminAction = createAdminAction(ticketReference,
                                                                    adminActionProperties.comments(),
                                                                    adminActionProperties.hiddenReason(),
                                                                    userAccount);
            adminAction.setMedia(media);
            adminActionRepository.saveAndFlush(adminAction);

            media.setObjectAdminAction(adminAction);
            media.setHidden(true);
            mediaRepository.saveAndFlush(media);
        }

        return mediaVersions;
    }

    private ObjectAdminActionEntity createAdminAction(String ticketReference,
                                                      String comments,
                                                      ObjectHiddenReasonEntity hiddenReason,
                                                      UserAccountEntity userAccount) {
        var objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setTicketReference(ticketReference);
        objectAdminActionEntity.setComments(comments);
        objectAdminActionEntity.setObjectHiddenReason(hiddenReason);
        objectAdminActionEntity.setHiddenBy(userAccount);
        objectAdminActionEntity.setHiddenDateTime(currentTimeHelper.currentOffsetDateTime());
        objectAdminActionEntity.setMarkedForManualDeletion(false);

        return objectAdminActionEntity;
    }

}
