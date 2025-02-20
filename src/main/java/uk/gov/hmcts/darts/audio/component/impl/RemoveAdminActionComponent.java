package uk.gov.hmcts.darts.audio.component.impl;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveAdminActionComponent {

    private final AuditApi auditApi;

    private final MediaRepository mediaRepository;
    private final ObjectAdminActionRepository adminActionRepository;

    public static final String AUDIT_TEMPLATE = "Media id: %d, Ticket ref: %s, Comments: %s";

    @Transactional
    public void removeAdminAction(@NonNull MediaEntity targetedMedia) {
        List<MediaEntity> allMediaVersions;
        if (StringUtils.isBlank(targetedMedia.getChronicleId())) {
            log.debug("Attempting to remove all admin actions from non-versioned media with id {}",
                      targetedMedia.getId());
            allMediaVersions = Collections.singletonList(targetedMedia);
        } else {
            log.debug("Attempting to remove all admin actions from all versioned medias with chronicle id {}",
                      targetedMedia.getChronicleId());
            allMediaVersions = mediaRepository.findAllByChronicleId(targetedMedia.getChronicleId());
        }

        removeAdminAction(allMediaVersions);
    }

    @Transactional
    public void removeAdminAction(@NonNull List<MediaEntity> mediaVersions) {
        Set<String> uniqueChronicleIds = mediaVersions.stream()
            .map(MediaEntity::getChronicleId)
            .collect(Collectors.toSet());
        if (uniqueChronicleIds.size() != 1) {
            throw new IllegalStateException("All media versions must have the same chronicle id");
        }

        for (MediaEntity media : mediaVersions) {
            log.debug("Attempting to remove all admin actions from media id {}",
                      media.getId());
            Optional<ObjectAdminActionEntity> adminActionOptional = media.getObjectAdminAction();
            if (media.isHidden() || adminActionOptional.isPresent()) {
                String comments = null;
                String ticketReference = null;
                if (adminActionOptional.isPresent()) {
                    ObjectAdminActionEntity adminAction = adminActionOptional.get();
                    comments = adminAction.getComments();
                    ticketReference = adminAction.getTicketReference();
                }

                auditApi.record(AuditActivity.UNHIDE_AUDIO, AUDIT_TEMPLATE.formatted(media.getId(), ticketReference, comments));

                adminActionOptional.ifPresent(objectAdminActionEntity ->
                                                  adminActionRepository.deleteById(objectAdminActionEntity.getId()));

                media.setHidden(false);
                mediaRepository.saveAndFlush(media);
            }
        }
    }

}
