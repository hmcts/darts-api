package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class MediaStubComposable {

    private final MediaRepository mediaRepository;

    private final DartsPersistence dartsPersistence;

    private final UserAccountRepository userAccountRepository;

    private static final OffsetDateTime MEDIA_1_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_1_END_TIME = MEDIA_1_START_TIME.plusHours(1);

    public MediaEntity createMediaEntity(CourthouseStubComposable courthouseStubComposable,
                                         CourtroomStubComposable courtroomStub,
                                         String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                         String mediaType) {

        CourtroomEntity courtroom = courtroomStub
            .createCourtroomUnlessExists(courthouseStubComposable, courthouseName, courtroomName, userAccountRepository.getReferenceById(0));
        return dartsPersistence.save(PersistableFactory.getMediaTestData().createMediaWith(courtroom, startTime, endTime, channel, mediaType));
    }

    public MediaEntity createMediaEntity(CourtroomStubComposable courtroomStub,
                                         CourthouseEntity courthouse,
                                         String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                         String mediaType) {
        CourtroomEntity courtroom = courtroomStub
            .createCourtroomUnlessExists(courthouse, courtroomName, userAccountRepository.getReferenceById(0));
        return dartsPersistence.save(PersistableFactory.getMediaTestData().createMediaWith(courtroom, startTime, endTime, channel, mediaType));
    }

    public MediaEntity createMediaEntity(CourtroomStubComposable courtroomStub,
                                         CourthouseEntity courthouse, String courtroomName,
                                         OffsetDateTime startTime, OffsetDateTime endTime, int channel, boolean isCurrent) {
        CourtroomEntity courtroom = courtroomStub
            .createCourtroomUnlessExists(courthouse, courtroomName, userAccountRepository.getReferenceById(0));
        MediaEntity mediaEntity = PersistableFactory.getMediaTestData().createMediaWith(courtroom, startTime, endTime, channel, "mp2");
        mediaEntity.setIsCurrent(isCurrent);

        return dartsPersistence.save(mediaEntity);
    }

    public MediaEntity createMediaEntity(CourthouseStubComposable courthouseStubComposable,
                                         CourtroomStubComposable courtroomStub,
                                         String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaEntity(courthouseStubComposable, courtroomStub,
                                 courthouseName, courtroomName, startTime, endTime, channel, "mp2");
    }

    public MediaEntity createMediaEntity(CourthouseStubComposable courthouseStubComposable,
                                         CourtroomStubComposable courtroomStub,
                                         String courthouseName, String courtroomName,
                                         OffsetDateTime startTime, OffsetDateTime endTime,
                                         int channel, boolean isCurrent) {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists(
            courthouseStubComposable, courthouseName, courtroomName, userAccountRepository.getReferenceById(0)
        );
        MediaEntity mediaEntity = PersistableFactory.getMediaTestData()
            .createMediaWith(courtroom, startTime, endTime, channel, "mp2");
        mediaEntity.setIsCurrent(isCurrent);
        return dartsPersistence.save(mediaEntity);
    }



    public MediaEntity createHiddenMediaEntity(CourthouseStubComposable courthouseStubComposable,
                                               CourtroomStubComposable courtroomStub,
                                               String courthouseName, String courtroomName,
                                               OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                               String mediaType) {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists(
            courthouseStubComposable, courthouseName, courtroomName, userAccountRepository.getReferenceById(0));
        MediaEntity mediaEntity = PersistableFactory.getMediaTestData().createMediaWith(courtroom, startTime, endTime, channel, mediaType);
        mediaEntity.setHidden(true);
        return dartsPersistence.save(mediaEntity);
    }

    public MediaEntity createAndSaveMedia(CourthouseStubComposable courthouseStubComposable, CourtroomStubComposable courtroomStub) {
        return createMediaEntity(courthouseStubComposable, courtroomStub, "testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 1);
    }

}