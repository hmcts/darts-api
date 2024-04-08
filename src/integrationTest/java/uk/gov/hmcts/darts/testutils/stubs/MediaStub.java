package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.testutils.data.MediaTestData.createMediaWith;

@Component
@RequiredArgsConstructor
public class MediaStub {

    private static final OffsetDateTime MEDIA_1_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_1_END_TIME = MEDIA_1_START_TIME.plusHours(1);
    private static final OffsetDateTime MEDIA_2_START_TIME = OffsetDateTime.parse("2023-01-01T16:00:00Z");
    private static final OffsetDateTime MEDIA_2_END_TIME = MEDIA_2_START_TIME.plusHours(1);

    private final MediaRepository mediaRepository;
    private final CourtroomStub courtroomStub;

    public MediaEntity createMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
    String mediaType) {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists(courthouseName, courtroomName);
        return mediaRepository.saveAndFlush(createMediaWith(courtroom, startTime, endTime, channel, mediaType));
    }

    public MediaEntity createMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaEntity(courthouseName, courtroomName, startTime, endTime, channel, "mp2");
    }

    public List<MediaEntity> createAndSaveSomeMedias() {
        return List.of(
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 1),
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 2),
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 3),
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 4),
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_2_START_TIME, MEDIA_2_END_TIME, 1),
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_2_START_TIME, MEDIA_2_END_TIME, 2),
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_2_START_TIME, MEDIA_2_END_TIME, 3),
            createMediaEntity("testCourthouse", "testCourtroom", MEDIA_2_START_TIME, MEDIA_2_END_TIME, 4)
       );
    }

    public MediaEntity createAndSaveMedia() {
        return createMediaEntity("testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 1);
    }
}
