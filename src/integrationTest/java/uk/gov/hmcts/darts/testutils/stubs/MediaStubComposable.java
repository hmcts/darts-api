package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.test.common.data.MediaTestData.createMediaWith;

@Component
@RequiredArgsConstructor
public class MediaStubComposable {

    private final MediaRepository mediaRepository;

    private final UserAccountRepository userAccountRepository;

    private static final OffsetDateTime MEDIA_1_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_1_END_TIME = MEDIA_1_START_TIME.plusHours(1);
    private static final OffsetDateTime MEDIA_2_START_TIME = OffsetDateTime.parse("2023-01-01T16:00:00Z");
    private static final OffsetDateTime MEDIA_2_END_TIME = MEDIA_2_START_TIME.plusHours(1);

    public MediaEntity createMediaEntity(CourthouseStubComposable courthouseStubComposable,
                                         CourtroomStubComposable courtroomStub,
                                         String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                         String mediaType) {

        CourtroomEntity courtroom = courtroomStub
            .createCourtroomUnlessExists(courthouseStubComposable, courthouseName, courtroomName, userAccountRepository.getReferenceById(0));
        return mediaRepository.saveAndFlush(createMediaWith(courtroom, startTime, endTime, channel, mediaType));
    }

    public MediaEntity createMediaEntity(CourtroomStubComposable courtroomStub,
                                         CourthouseEntity courthouse,
                                         String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                         String mediaType) {
        CourtroomEntity courtroom = courtroomStub
            .createCourtroomUnlessExists(courthouse, courtroomName, userAccountRepository.getReferenceById(0));
        return mediaRepository.saveAndFlush(createMediaWith(courtroom, startTime, endTime, channel, mediaType));
    }

    public MediaEntity createMediaEntity(CourtroomStubComposable courtroomStub,
                                         CourthouseEntity courthouse, String courtroomName,
                                         OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        CourtroomEntity courtroom = courtroomStub
            .createCourtroomUnlessExists(courthouse, courtroomName, userAccountRepository.getReferenceById(0));
        return mediaRepository.saveAndFlush(createMediaWith(courtroom, startTime, endTime, channel, "mp2"));
    }

    public MediaEntity createMediaEntity(CourthouseStubComposable courthouseStubComposable,
                                         CourtroomStubComposable courtroomStub,
                                         String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaEntity(courthouseStubComposable, courtroomStub,
                                 courthouseName, courtroomName, startTime, endTime, channel, "mp2");
    }


    public MediaEntity createHiddenMediaEntity(CourthouseStubComposable courthouseStubComposable,
                                               CourtroomStubComposable courtroomStub,
                                               String courthouseName, String courtroomName,
                                               OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                               String mediaType) {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists(
            courthouseStubComposable, courthouseName, courtroomName, userAccountRepository.getReferenceById(0));
        MediaEntity mediaEntity = createMediaWith(courtroom, startTime, endTime, channel, mediaType);
        mediaEntity.setHidden(true);
        return mediaRepository.saveAndFlush(mediaEntity);
    }

    public MediaEntity createAndSaveMedia(CourthouseStubComposable courthouseStubComposable, CourtroomStubComposable courtroomStub) {
        return createMediaEntity(courthouseStubComposable, courtroomStub, "testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 1);
    }

}