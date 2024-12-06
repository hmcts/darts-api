package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaLinkedCaseHelper {

    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;
    private final HearingRepository hearingRepository;

    public void linkMediaToCase(MediaEntity mediaEntity, CourtCaseEntity courtCase, MediaLinkedCaseSourceType sourceType, UserAccountEntity createdBy) {
        if (!mediaLinkedCaseRepository.existsByMediaAndCourtCase(mediaEntity, courtCase)) {
            MediaLinkedCaseEntity mediaLinkedCaseEntity = new MediaLinkedCaseEntity(mediaEntity, courtCase, createdBy, sourceType);
            mediaLinkedCaseEntity = mediaLinkedCaseRepository.saveAndFlush(mediaLinkedCaseEntity);
            log.debug("cas_id {} and med_id {} were not linked, Created new link via mlc_id {}",
                      courtCase.getId(), mediaEntity.getId(), mediaLinkedCaseEntity.getId());
        } else {
            log.debug("med_id {} and cas_id {} already linked, nothing to do",
                      mediaEntity.getId(), courtCase.getId());
        }
    }

    public void linkMediaByEvent(EventEntity event, MediaEntity mediaEntity, MediaLinkedCaseSourceType sourceType, UserAccountEntity userAccount) {
        Set<HearingEntity> hearingsToSave = new HashSet<>();
        event.getHearingEntities().forEach(hearingEntity -> {
            try {
                if (!hearingEntity.containsMedia(mediaEntity)) {
                    hearingEntity.addMedia(mediaEntity);
                    hearingsToSave.add(hearingEntity);

                    linkMediaToCase(mediaEntity, hearingEntity.getCourtCase(), sourceType, userAccount);
                    log.info("Linking media {} to hearing {} through eveId {}", mediaEntity.getId(), hearingEntity.getId(), event.getId());
                }
            } catch (Exception e) {
                log.error("Error linking media {} to hearing {} through eveId {}", mediaEntity.getId(), hearingEntity.getId(), event.getId(), e);
            }
        });
        hearingRepository.saveAll(hearingsToSave);
    }
}