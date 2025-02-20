package uk.gov.hmcts.darts.audio.component.impl;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveAdminActionComponent {

    private final AuditApi auditApi;

    private final MediaRepository mediaRepository;
    private final ObjectAdminActionRepository adminActionRepository;

    public static final String AUDIT_TEMPLATE = "Media id: %d, Ticket ref: %s, Comments: %s";

    @Transactional
    public void removeAdminAction(@NonNull List<MediaEntity> mediaVersions) {
        List<String> chronicleIds = mediaVersions.stream()
            .map(MediaEntity::getChronicleId)
            .filter(Objects::nonNull)
            .distinct()
            .filter(chronicleId -> !chronicleId.isEmpty())
            .toList();
        if (chronicleIds.size() != 1) {
            throw new IllegalStateException("All media versions must have the same chronicle id");
        }

        String chronicleId = chronicleIds.getFirst();
        log.debug("Attempting to remove all admin actions from all medias with chronicle id {}",
                  chronicleId);

        for (MediaEntity media : mediaVersions) {
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
