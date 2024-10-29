package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaLinkedCaseHelper {

    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;

    public void addCase(MediaEntity mediaEntity, CourtCaseEntity courtCase, MediaLinkedCaseSourceType sourceType, UserAccountEntity createdBy) {
        List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = mediaLinkedCaseRepository.findByMedia(mediaEntity);
        List<CourtCaseEntity> linkedCases = mediaLinkedCaseEntities.stream().map(MediaLinkedCaseEntity::getCourtCase).toList();
        if (log.isDebugEnabled()) {
            log.debug("Handling med_id {} and cas_id {}. Currently the media has the following cas_ids linked: {}",
                      mediaEntity.getId(),
                      courtCase.getId(),
                      linkedCases.stream()
                          .map(CourtCaseEntity::getId)
                          .toList());
        }
        if (!linkedCases.contains(courtCase)) {
            MediaLinkedCaseEntity mediaLinkedCaseEntity = new MediaLinkedCaseEntity(mediaEntity, courtCase, createdBy, sourceType);
            mediaLinkedCaseRepository.saveAndFlush(mediaLinkedCaseEntity);
            log.debug("cas_id {} and med_id {} were not linked, Created new link via mlc_id {}",
                      courtCase.getId(), mediaEntity.getId(), mediaLinkedCaseEntity.getId());
        } else {
            log.debug("med_id {} and cas_id {} already linked, nothing to do",
                      mediaEntity.getId(), courtCase.getId());
        }
    }
}