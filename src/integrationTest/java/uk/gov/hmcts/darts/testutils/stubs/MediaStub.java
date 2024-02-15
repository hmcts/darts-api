package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.testutils.data.MediaTestData.createMediaWith;

@Component
@RequiredArgsConstructor
public class MediaStub {

    private final MediaRepository mediaRepository;
    private final CourtroomStub courtroomStub;

    public MediaEntity createMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists(courthouseName, courtroomName);
        return mediaRepository.saveAndFlush(createMediaWith(courtroom, startTime, endTime, channel));
    }

}
