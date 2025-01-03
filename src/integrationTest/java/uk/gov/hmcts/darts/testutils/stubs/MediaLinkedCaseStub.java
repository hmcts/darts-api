package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;

@Component
@RequiredArgsConstructor
public class MediaLinkedCaseStub {

    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;

    public MediaLinkedCaseEntity createCaseLinkedMedia(MediaEntity media, CourtCaseEntity caseEntity) {
        MediaLinkedCaseEntity mediaLinkedCaseEntity = new MediaLinkedCaseEntity();
        mediaLinkedCaseEntity.setMedia(media);
        mediaLinkedCaseEntity.setCourtCase(caseEntity);
        return mediaLinkedCaseRepository.save(mediaLinkedCaseEntity);
    }
}