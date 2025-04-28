package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaSubStringQueryEnum;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Deprecated
public class MediaStub {

    private static final OffsetDateTime MEDIA_1_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_1_END_TIME = MEDIA_1_START_TIME.plusHours(1);
    private static final OffsetDateTime MEDIA_2_START_TIME = OffsetDateTime.parse("2023-01-01T16:00:00Z");
    private static final OffsetDateTime MEDIA_2_END_TIME = MEDIA_2_START_TIME.plusHours(1);

    private final HearingRepository hearingRepository;
    private final MediaRepository mediaRepository;
    private final UserAccountRepository userAccountRepository;
    private final HearingStub hearingStub;
    private final MediaStubComposable mediaStubComposable;
    private final CourthouseStubComposable courthouseStubComposable;
    private final CourtroomStubComposable courtroomStub;

    private static final String CASE_NUMBER_PREFIX = "CaseNumber";
    private final RetrieveCoreObjectService retrieveCoreObjectService;

    @Transactional
    public MediaEntity createMediaEntity(String courthouseName, String courtroomName,
                                         OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                         String mediaType) {
        return mediaStubComposable.createMediaEntity(courthouseStubComposable,
                                                     courtroomStub, courthouseName, courtroomName, startTime, endTime, channel, mediaType);
    }

    public MediaEntity createMediaEntity(CourthouseEntity courthouse, String courtroomName,
                                         OffsetDateTime startTime, OffsetDateTime endTime, int channel,
                                         String mediaType) {
        return mediaStubComposable.createMediaEntity(courtroomStub, courthouse, courtroomName, startTime, endTime, channel, mediaType);
    }

    public MediaEntity createMediaEntity(CourthouseEntity courthouse, String courtroomName,
                                         OffsetDateTime startTime, OffsetDateTime endTime, int channel, boolean isCurrent) {
        return mediaStubComposable.createMediaEntity(courtroomStub, courthouse, courtroomName, startTime, endTime, channel, isCurrent);
    }

    public MediaEntity createMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return mediaStubComposable.createMediaEntity(courthouseStubComposable,
                                                     courtroomStub, courthouseName, courtroomName, startTime, endTime, channel);
    }

    @Transactional
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

    @Transactional
    public MediaEntity createAndSaveMedia() {
        return createMediaEntity("testCourthouse", "testCourtroom", MEDIA_1_START_TIME, MEDIA_1_END_TIME, 1);
    }


    /**
     * generates media test data. The following will be used for generation:-
     * Unique court house with unique name for each media
     * Unique case number with unique case number for each  media
     * Unique unique hearing with hearing date starting with today and incrementing by day for each transformed media record
     * Unique media with start date hours decremented for each media and end date hours incremented for each media
     *
     * @param count The number of  media objects that are to be generated
     * @return The list of generated media in chronological order
     */
    @Transactional
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

            hearingStub.createHearingWithMedia(courtName, courtName, caseNumber, hearingDate, mediaEntity);


            hoursBefore = hoursBefore.minusHours(1);
            hoursAfter = hoursAfter.plusHours(1);
            hearingDate = hearingDate.plusDays(1);

            retMediaList.add(mediaRepository.findById(mediaEntity.getId()).get());
        }

        return retMediaList;
    }

    public void linkToCase(MediaEntity media, String caseNumber) {

        String courthouseName = media.getCourtroom().getCourthouse().getCourthouseName();
        String courtroomName = media.getCourtroom().getName();
        HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, media.getStart().toLocalDateTime(),
                                                                                  userAccountRepository.getReferenceById(0));
        hearing.addMedia(media);
        hearingRepository.saveAndFlush(hearing);
    }

    @Transactional
    public Integer getHearingId(Long id) {
        Optional<MediaEntity> mediaEntity = mediaRepository.findById(id);
        return mediaEntity.get().getHearing().getId();
    }
}