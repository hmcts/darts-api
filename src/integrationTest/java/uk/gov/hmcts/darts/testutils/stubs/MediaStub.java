package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.repository.TransformedMediaSubStringQueryEnum;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.darts.test.common.data.MediaTestData.createMediaWith;

@Component
@RequiredArgsConstructor
public class MediaStub {

    private static final OffsetDateTime MEDIA_1_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_1_END_TIME = MEDIA_1_START_TIME.plusHours(1);
    private static final OffsetDateTime MEDIA_2_START_TIME = OffsetDateTime.parse("2023-01-01T16:00:00Z");
    private static final OffsetDateTime MEDIA_2_END_TIME = MEDIA_2_START_TIME.plusHours(1);

    private final MediaRepository mediaRepository;
    private final CourtroomStub courtroomStub;
    private final UserAccountRepository userAccountRepository;
    private final UserAccountStub userAccountStub;
    private final HearingStub hearingStub;

    private static final String FILE_NAME_PREFIX = "FileName";

    private static final String CASE_NUMBER_PREFIX = "CaseNumber";

    public MediaEntity createMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                         String mediaType) {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists(courthouseName, courtroomName, userAccountRepository.getReferenceById(0));
        return mediaRepository.saveAndFlush(createMediaWith(courtroom, startTime, endTime, channel, mediaType));
    }

    public MediaEntity createMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return createMediaEntity(courthouseName, courtroomName, startTime, endTime, channel, "mp2");
    }

    public MediaEntity createHiddenMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                               String mediaType) {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists(courthouseName, courtroomName, userAccountRepository.getReferenceById(0));
        MediaEntity mediaEntity = createMediaWith(courtroom, startTime, endTime, channel, mediaType);
        mediaEntity.setHidden(true);
        return mediaRepository.saveAndFlush(mediaEntity);
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


    /**
     * generates media test data. The following will be used for generation:-
     * Unique court house with unique name for each media
     * Unique case number with unique case number for each  media
     * Unique unique hearing with hearing date starting with today and incrementing by day for each transformed media record
     * Unique media with start date hours decremented for each media and end date hours incremented for each media
     * @param count The number of  media objects that are to be generated
     * @return The list of generated media in chronological order
     */
    public List<MediaEntity> generateMediaEntities(int count) {
        List<MediaEntity> retMediaList = new ArrayList<>();
        OffsetDateTime hoursBefore = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime hoursAfter = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDateTime hearingDate = LocalDateTime.now(ZoneOffset.UTC);

        for (int transformedMediaCount = 0; transformedMediaCount < count; transformedMediaCount++) {
            String courtName = TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryString(Integer.toString(transformedMediaCount));
            String caseNumber = CASE_NUMBER_PREFIX + transformedMediaCount;

            MediaEntity mediaEntity = this.createMediaEntity(courtName, caseNumber, hoursBefore, hoursAfter, 1);
            mediaRepository.save(mediaEntity);

            hearingStub.createHearingWithMedia(courtName, courtName,
                                                              caseNumber, hearingDate, mediaEntity);


            hoursBefore = hoursBefore.minusHours(1);
            hoursAfter = hoursAfter.plusHours(1);
            hearingDate = hearingDate.plusDays(1);

            retMediaList.add(mediaRepository.findById(mediaEntity.getId()).get());
        }

        return retMediaList;
    }

    @Transactional
    public Integer getHearingId(Integer id) {
        Optional<MediaEntity> mediaEntity = mediaRepository.findById(id);
        return mediaEntity.get().getHearingList().get(0).getId();
    }
}