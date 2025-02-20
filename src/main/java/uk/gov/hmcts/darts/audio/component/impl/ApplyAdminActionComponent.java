package uk.gov.hmcts.darts.audio.component.impl;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private final ObjectHiddenReasonRepository hiddenReasonRepository;

    private final AuditApi auditApi;

    public static final String AUDIT_TEMPLATE = "Media id: %d; Ticket ref: %s";

    @Transactional
    public List<MediaEntity> applyAdminAction(@NonNull MediaEntity targetedMedia,
                                              @NonNull AdminActionRequest adminActionRequest) {

        List<MediaEntity> allMediaVersions;
        if (StringUtils.isBlank(targetedMedia.getChronicleId())) {
            log.debug("Attempting to apply admin action with ticket ref {} to non-versioned media with id {}",
                      adminActionRequest.getTicketReference(),
                      targetedMedia.getId());
            allMediaVersions = Collections.singletonList(targetedMedia);
        } else {
            log.debug("Attempting to apply admin action with ticket ref {} to all versioned medias with chronicle id {}",
                      adminActionRequest.getTicketReference(),
                      targetedMedia.getChronicleId());
            allMediaVersions = mediaRepository.findAllByChronicleId(targetedMedia.getChronicleId());
        }

        // We need to first remove any admin actions that may be linked to any version of the targeted media so that we can then link a new admin action
        // reflecting the details in the incoming adminActionRequest
        removeAdminActionComponent.removeAdminAction(allMediaVersions);

        final String ticketReference = adminActionRequest.getTicketReference();
        final String comments = adminActionRequest.getComments();
        final ObjectHiddenReasonEntity objectHiddenReason = hiddenReasonRepository.findById(adminActionRequest.getReasonId())
            .orElseThrow(() -> new DartsApiException(AudioApiError.MEDIA_HIDE_ACTION_REASON_NOT_FOUND));
        final UserAccountEntity userAccount = userIdentity.getUserAccount();

        for (MediaEntity media : allMediaVersions) {
            auditApi.record(HIDE_AUDIO, AUDIT_TEMPLATE.formatted(media.getId(), ticketReference));

            ObjectAdminActionEntity adminAction = createAdminAction(ticketReference,
                                                                    comments,
                                                                    objectHiddenReason,
                                                                    userAccount);
            adminAction.setMedia(media);
            adminActionRepository.saveAndFlush(adminAction);

            media.setObjectAdminAction(adminAction);
            media.setHidden(true);
            mediaRepository.saveAndFlush(media);
        }

        return allMediaVersions;
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
