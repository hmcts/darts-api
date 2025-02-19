package uk.gov.hmcts.darts.audio.component.impl;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

import java.util.List;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.HIDE_AUDIO;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyAdminActionComponent {

    private final AuditApi auditApi;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;

    private final MediaRepository mediaRepository;
    private final ObjectAdminActionRepository adminActionRepository;
    private final ObjectHiddenReasonRepository hiddenReasonRepository;

    private final RemoveAdminActionComponent removeAdminActionComponent;

    public static final String AUDIT_TEMPLATE = "Media id: %d; Ticket ref: %s";

    @Transactional
    public ObjectAdminActionEntity applyAdminActionFromAllVersions(@NonNull MediaEntity targetedMedia,
                                                                   @NonNull List<MediaEntity> otherMediaVersions,
                                                                   @NonNull AdminActionRequest adminActionRequest) {
        log.debug("Attempting to apply admin action {} to all media versions with chronicle id {}",
                  adminActionRequest,
                  targetedMedia.getChronicleId());

        final String ticketReference = adminActionRequest.getTicketReference();

        auditApi.record(HIDE_AUDIO, AUDIT_TEMPLATE.formatted(targetedMedia.getId(), ticketReference));

        // We need to first remove any admin actions that are linked to any version of the targeted media so that we can link a new admin action reflecting
        // the details in the incoming adminActionRequest
        removeAdminActionComponent.removeAdminActionFromAllVersions(targetedMedia);

        targetedMedia.setHidden(true);
        mediaRepository.saveAndFlush(targetedMedia);

        final ObjectHiddenReasonEntity objectHiddenReason = hiddenReasonRepository.findById(adminActionRequest.getReasonId())
            .orElseThrow(() -> new DartsApiException(AudioApiError.MEDIA_HIDE_ACTION_REASON_NOT_FOUND));
        final UserAccountEntity userAccount = userIdentity.getUserAccount();
        final String comments = adminActionRequest.getComments();

        final ObjectAdminActionEntity adminActionForTargetedMedia = createAndLinkAdminAction(ticketReference,
                                                                                       comments,
                                                                                       targetedMedia,
                                                                                       objectHiddenReason,
                                                                                       userAccount);
        adminActionRepository.saveAndFlush(adminActionForTargetedMedia);

        List<MediaEntity> allOtherMediaVersions = mediaRepository.findAllByChronicleId(targetedMedia.getChronicleId()).stream()
            .filter(media -> !media.getId().equals(targetedMedia.getId()))
            .toList();
        if (allOtherMediaVersions.isEmpty()) {
            return adminActionForTargetedMedia;
        }

        // adminActionRepository.deleteObjectAdminActionEntitiesByMedias(allOtherMediaVersions);
        for (MediaEntity media : allOtherMediaVersions) {
            auditApi.record(HIDE_AUDIO, AUDIT_TEMPLATE.formatted(media.getId(), ticketReference));
            media.setHidden(true);
        }
        mediaRepository.saveAllAndFlush(allOtherMediaVersions);

        List<ObjectAdminActionEntity> adminActions = allOtherMediaVersions.stream()
            .map(media -> createAndLinkAdminAction(ticketReference,
                                                   comments,
                                                   media,
                                                   objectHiddenReason,
                                                   userAccount))
            .toList();
        adminActionRepository.saveAllAndFlush(adminActions);

        return adminActionForTargetedMedia;
    }

    private ObjectAdminActionEntity createAndLinkAdminAction(String ticketReference,
                                                             String comments,
                                                             MediaEntity media,
                                                             ObjectHiddenReasonEntity hiddenReason,
                                                             UserAccountEntity userAccount) {
        var objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setTicketReference(ticketReference);
        objectAdminActionEntity.setComments(comments);
        objectAdminActionEntity.setMedia(media);
        objectAdminActionEntity.setObjectHiddenReason(hiddenReason);
        objectAdminActionEntity.setHiddenBy(userAccount);
        objectAdminActionEntity.setHiddenDateTime(currentTimeHelper.currentOffsetDateTime());
        objectAdminActionEntity.setMarkedForManualDeletion(false);

        return objectAdminActionEntity;
    }

}
