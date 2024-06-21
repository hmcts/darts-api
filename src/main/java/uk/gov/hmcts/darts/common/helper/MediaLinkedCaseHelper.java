package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MediaLinkedCaseHelper {

    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;

    public void addCase(MediaEntity mediaEntity, CourtCaseEntity courtCase) {
        List<CourtCaseEntity> linkedCases = mediaEntity.getCases();
        if (!linkedCases.contains(courtCase)) {
            MediaLinkedCaseEntity mediaLinkedCaseEntity = new MediaLinkedCaseEntity();
            mediaLinkedCaseEntity.setMedia(mediaEntity);
            mediaLinkedCaseEntity.setCourtCase(courtCase);
            mediaLinkedCaseRepository.saveAndFlush(mediaLinkedCaseEntity);
        }
    }
}
