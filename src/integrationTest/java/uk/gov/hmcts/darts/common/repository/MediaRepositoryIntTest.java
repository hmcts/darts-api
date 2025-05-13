package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    HearingRepository hearingRepository;

    @Autowired
    MediaRepository mediaRepository;

    @Autowired
    MediaLinkedCaseRepository mediaLinkedCaseRepository;

    @Autowired
    private MediaStub mediaStub;

    private List<MediaEntity> generatedMediaEntities;

    private static final int GENERATION_COUNT = 20;

    @BeforeEach
    public void before() {
        generatedMediaEntities = mediaStub.generateMediaEntities(GENERATION_COUNT);
    }

    @Test
    void testFindMediasByCaseId() {
        // given
        var caseA = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        var hearA1 = PersistableFactory.getHearingTestData().createHearingFor(caseA);
        var hearA2 = PersistableFactory.getHearingTestData().createHearingFor(caseA);
        var hearA3 = PersistableFactory.getHearingTestData().createHearingFor(caseA);

        var caseB = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        var hearB = PersistableFactory.getHearingTestData().createHearingFor(caseB);

        var media0 = PersistableFactory.getMediaTestData().someMinimalMedia();
        var media1 = PersistableFactory.getMediaTestData().someMinimalMedia();
        var media2 = PersistableFactory.getMediaTestData().someMinimalMedia();

        hearA1.addMedia(media0);
        hearA1.addMedia(media1);
        hearA2.addMedia(media2);
        hearB.addMedia(media0);

        dartsPersistence.saveAll(hearA1, hearA2, hearA3, hearB);

        // when
        var caseAMedias = mediaRepository.findAllByCaseId(caseA.getId());
        var caseBMedias = mediaRepository.findAllByCaseId(caseB.getId());

        dartsPersistence.getTransactionalUtil().executeInTransaction(() -> {
            var foundMedia0 = mediaRepository.findById(media0.getId()).orElseThrow();
            var foundMedia1 = mediaRepository.findById(media1.getId()).orElseThrow();

            // then
            var caseAMediasId = caseAMedias.stream().map(MediaEntity::getId);
            assertThat(caseAMediasId).containsExactlyInAnyOrder(foundMedia0.getId(), foundMedia1.getId(), media2.getId());
            var caseBMediasId = caseBMedias.stream().map(MediaEntity::getId);
            assertThat(caseBMediasId).containsExactlyInAnyOrder(foundMedia0.getId());

            List<CourtCaseEntity> media0cases = foundMedia0.associatedCourtCases();
            assertThat(media0cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId(), caseB.getId());
            List<CourtCaseEntity> media1cases = foundMedia1.associatedCourtCases();
            assertThat(media1cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId());
        });
    }

    @Test
    void testFindAllMediaDetailsWithoutParameters() {
        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(null, null, null);
        assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
    }

    @Test
    void testFindMediaDetailsWithHearingId() {
        MediaEntity expectedMedia = generatedMediaEntities.getFirst();
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            List.of(hearingId), null, null);
        assertEquals(1, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindMediaDetailsWithHearingIdAndStartDate() {
        MediaEntity expectedMedia = generatedMediaEntities.getFirst();
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());
        OffsetDateTime startTime = expectedMedia.getStart();

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(List.of(hearingId), startTime, null);
        assertEquals(1, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindMediaDetailsWithHearingIdAndStartDateAndEndDate() {
        MediaEntity expectedMedia = generatedMediaEntities.getFirst();
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());
        OffsetDateTime startTime = expectedMedia.getStart();
        OffsetDateTime endTime = expectedMedia.getEnd();
        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(List.of(hearingId), startTime, endTime);
        assertEquals(1, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindMediaDetailsWithStartDateAndEndDate() {
        // get all records before record 10 based on the date
        MediaEntity expectedMedia = generatedMediaEntities.get(10);

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            null, expectedMedia.getStart(), expectedMedia.getEnd());
        assertEquals(11, transformedMediaEntityList.size());

        for (int results = 0; results < transformedMediaEntityList.size(); results++) {
            Long expectedId = generatedMediaEntities.get(results).getId();
            assertEquals(expectedId, transformedMediaEntityList.get(results).getId());
        }
    }

    @Test
    void testFindMediaDetailsWithStartDate() {
        MediaEntity expectedMedia = generatedMediaEntities.get(1);

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            null, expectedMedia.getStart(), null);

        assertEquals(2, transformedMediaEntityList.size());
        assertEquals(generatedMediaEntities.getFirst().getId(), transformedMediaEntityList.getFirst().getId());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(1).getId());
    }

    @Test
    void testFindMediaDetailsWithStartDateAndEndDateAndHearingId() {
        MediaEntity expectedMedia = generatedMediaEntities.get(10);

        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            List.of(hearingId), expectedMedia.getStart(), expectedMedia.getEnd());
        assertEquals(1, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindMediaDetailsWithMultipleHearingIds() {
        MediaEntity expectedMedia = generatedMediaEntities.get(10);
        MediaEntity expectedMedia1 = generatedMediaEntities.get(11);

        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());
        Integer hearingId2 = mediaStub.getHearingId(expectedMedia1.getId());

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            List.of(hearingId, hearingId2), null, null);
        assertEquals(2, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.getFirst().getId());
        assertEquals(expectedMedia1.getId(), transformedMediaEntityList.get(1).getId());
    }

    @Test
    void findAllLinkedByMediaLinkedCaseByCaseId() {
        // given
        var caseA = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        var hearA1 = PersistableFactory.getHearingTestData().createHearingFor(caseA);
        var hearA2 = PersistableFactory.getHearingTestData().createHearingFor(caseA);
        var hearA3 = PersistableFactory.getHearingTestData().createHearingFor(caseA);

        var caseB = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        var hearB = PersistableFactory.getHearingTestData().createHearingFor(caseB);

        var caseC = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        var hearC = PersistableFactory.getHearingTestData().createHearingFor(caseC);

        var media1 = PersistableFactory.getMediaTestData().someMinimalMedia();
        var media2 = PersistableFactory.getMediaTestData().someMinimalMedia();
        var media3 = PersistableFactory.getMediaTestData().someMinimalMedia();

        hearA1.addMedia(media1);
        hearA1.addMedia(media2);
        hearA2.addMedia(media3);
        hearB.addMedia(media1);

        dartsPersistence.saveAll(hearA1, hearA2, hearA3, hearB, hearC);

        var mediaLinked1 = PersistableFactory.getMediaTestData().someMinimalMedia();
        var mediaLinked2 = PersistableFactory.getMediaTestData().someMinimalMedia();
        var mediaLinked3 = PersistableFactory.getMediaTestData().someMinimalMedia();

        dartsPersistence.save(mediaLinked1);
        dartsPersistence.save(mediaLinked2);
        dartsPersistence.save(mediaLinked3);

        mediaLinkedCaseRepository.save(createMediaLinkedCase(mediaLinked1, caseA));
        mediaLinkedCaseRepository.save(createMediaLinkedCase(mediaLinked2, caseA));
        mediaLinkedCaseRepository.save(createMediaLinkedCase(mediaLinked3, caseC));

        // when
        var linkedMediaForCaseA = mediaRepository.findAllLinkedByMediaLinkedCaseByCaseId(caseA.getId());
        var linkedMediaForCaseB = mediaRepository.findAllLinkedByMediaLinkedCaseByCaseId(caseB.getId());
        var linkedMediaForCaseC = mediaRepository.findAllLinkedByMediaLinkedCaseByCaseId(caseC.getId());

        // then
        assertEquals(2, linkedMediaForCaseA.size());
        assertEquals(0, linkedMediaForCaseB.size());
        assertEquals(1, linkedMediaForCaseC.size());
    }


    @Test
    void mediaRepositoryIntTest_shouldReutrnTheCountOfMatchingChronicleIds() {
        var media1 = PersistableFactory.getMediaTestData().someMinimalMedia();
        media1.setChronicleId("someIdSingle");
        dartsPersistence.save(media1);
        var media2 = PersistableFactory.getMediaTestData().someMinimalMedia();
        media2.setChronicleId("someIdMultiple");
        dartsPersistence.save(media2);
        var media3 = PersistableFactory.getMediaTestData().someMinimalMedia();
        media3.setChronicleId("someIdMultiple");
        dartsPersistence.save(media3);

        assertThat(mediaRepository.getVersionCount("someIdSingle")).isEqualTo(1);
        assertThat(mediaRepository.getVersionCount("someIdMultiple")).isEqualTo(2);
    }

    static Stream<Arguments> findAllByCurrentMediaTimeContains() {
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime endTime = startTime.plusHours(2);

        return Stream.of(
            Arguments.of("Event time 30 mins before media start time should be returned when buffer is 30 min",
                         startTime, endTime, startTime.minusMinutes(30), true, true),
            Arguments.of("Event time 30 mins after media end time should be returned when buffer is 30 min",
                         startTime, endTime, endTime.plusMinutes(30), true, true),
            Arguments.of("Event time is within media time should be returned",
                         startTime, endTime, startTime.plusMinutes(30), true, true),
            Arguments.of("Event time 31 mins before media start time should not be returned when buffer is 30 min",
                         startTime, endTime, startTime.minusMinutes(31), false, true),
            Arguments.of("Event time 31 mins after media end time should not be returned when buffer is 30 min",
                         startTime, endTime, endTime.plusMinutes(31), false, true),
            Arguments.of("Event time is within media time and media is not current should not be returned",
                         startTime, endTime, startTime.plusMinutes(30), false, false)
        );
    }

    @ParameterizedTest(name = "{0} (StartTime: {1}. EndTime: {2}. EventTime: {3}. ExpectDataToReturn: {4})")
    @MethodSource("findAllByCurrentMediaTimeContains")
    void findAllByCurrentMediaTimeContains_tests(String testName, OffsetDateTime startTime, OffsetDateTime endTime, OffsetDateTime eventTime,
                                                 boolean expectDataToReturn, boolean isMediaCurrent) {
        CourtroomEntity courtroomEntity = PersistableFactory.getCourtroomTestData()
            .someMinimal();
        dartsPersistence.save(courtroomEntity);

        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .start(startTime)
            .end(endTime)
            .courtroom(courtroomEntity)
            .isCurrent(isMediaCurrent)
            .build()
            .getEntity();
        dartsPersistence.save(media);

        Duration buffer = Duration.ofMinutes(30);
        List<MediaEntity> medias = dartsDatabase.getMediaRepository().findAllByCurrentMediaTimeContains(
            courtroomEntity.getId(),
            eventTime.plus(buffer),
            eventTime.minus(buffer));

        if (expectDataToReturn) {
            assertEquals(1, medias.size());
            assertThat(medias.getFirst().getId()).isEqualTo(media.getId());
        } else {
            assertEquals(0, medias.size());
        }
    }

    private MediaLinkedCaseEntity createMediaLinkedCase(MediaEntity media, CourtCaseEntity courtCase) {
        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setMedia(media);
        mediaLinkedCase.setCourtCase(courtCase);
        return mediaLinkedCase;
    }

    @Test
    void findByCaseIdWithMediaList_shouldReturnCorrectData() {
        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        HearingEntity hearing3 = PersistableFactory.getHearingTestData().someMinimalHearing();

        hearing2.setCourtCase(hearing1.getCourtCase());

        dartsPersistence.save(hearing1);
        dartsPersistence.save(hearing2);
        dartsPersistence.save(hearing3);

        MediaEntity media1 = PersistableFactory.getMediaTestData().someMinimal();
        MediaEntity media2 = PersistableFactory.getMediaTestData().someMinimal();
        MediaEntity media3 = PersistableFactory.getMediaTestData().someMinimal();
        dartsPersistence.save(media1);
        dartsPersistence.save(media2);
        dartsPersistence.save(media3);

        hearing1.addMedia(media1);
        hearing2.addMedia(media2);
        hearing3.addMedia(media3);

        hearingRepository.saveAll(List.of(hearing1, hearing2, hearing3));

        List<MediaEntity> mediaEntities = mediaRepository.findByCaseIdWithMediaList(hearing1.getCourtCase().getId());

        assertThat(mediaEntities.stream().map(MediaEntity::getId))
            .hasSize(2)
            .containsExactlyInAnyOrder(media1.getId(), media2.getId());
    }


    @Test
    void findAllCurrentMediaByHearingId_whenIncludeHiddenIsTrue_allDataShouldBeReturned() {
        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();

        dartsPersistence.save(hearing1);
        dartsPersistence.save(hearing2);

        MediaEntity media1 = PersistableFactory.getMediaTestData().someMinimal();
        media1.setHidden(false);
        media1.setIsCurrent(true);
        MediaEntity media2 = PersistableFactory.getMediaTestData().someMinimal();
        media2.setHidden(false);
        media2.setIsCurrent(true);
        MediaEntity media3 = PersistableFactory.getMediaTestData().someMinimal();
        media3.setHidden(true);
        media3.setIsCurrent(true);

        dartsPersistence.save(media1);
        dartsPersistence.save(media2);
        dartsPersistence.save(media3);

        hearing1.addMedia(media1);
        hearing2.addMedia(media2);
        hearing2.addMedia(media3);

        hearingRepository.saveAll(List.of(hearing1, hearing2));

        List<MediaEntity> mediaEntities = mediaRepository.findAllCurrentMediaByHearingId(hearing2.getId(), true);

        assertThat(mediaEntities.stream().map(MediaEntity::getId))
            .hasSize(2)
            .containsExactlyInAnyOrder(media2.getId(), media3.getId());
    }

    @Test
    void findAllCurrentMediaByHearingId_whenIncludeHiddenIsFalse_onlyNonHiddenShouldBeReturned() {
        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();

        dartsPersistence.save(hearing1);
        dartsPersistence.save(hearing2);

        MediaEntity media1 = PersistableFactory.getMediaTestData().someMinimal();
        media1.setHidden(false);
        media1.setIsCurrent(true);
        MediaEntity media2 = PersistableFactory.getMediaTestData().someMinimal();
        media2.setHidden(false);
        media2.setIsCurrent(true);
        MediaEntity media3 = PersistableFactory.getMediaTestData().someMinimal();
        media3.setHidden(true);
        media3.setIsCurrent(true);

        dartsPersistence.save(media1);
        dartsPersistence.save(media2);
        dartsPersistence.save(media3);

        hearing1.addMedia(media1);
        hearing2.addMedia(media2);
        hearing2.addMedia(media3);

        hearingRepository.saveAll(List.of(hearing1, hearing2));

        List<MediaEntity> mediaEntities = mediaRepository.findAllCurrentMediaByHearingId(hearing2.getId(), false);

        assertThat(mediaEntities.stream().map(MediaEntity::getId))
            .hasSize(1)
            .containsExactlyInAnyOrder(media2.getId());
    }

    @Test
    void findByCaseIdAndIsCurrentTruePageable_ReturnsPaginatedList() {
        // given
        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsPersistence.save(hearing1);

        MediaEntity media1 = PersistableFactory.getMediaTestData().someMinimal();
        media1.setChannel(1);
        media1.setCourtroom(hearing1.getCourtroom());
        media1.setIsCurrent(true);
        dartsPersistence.save(media1);

        MediaEntity media2 = PersistableFactory.getMediaTestData().someMinimal();
        media2.setChannel(2);
        media2.setCourtroom(hearing1.getCourtroom());
        media2.setIsCurrent(true);
        dartsPersistence.save(media2);

        MediaEntity media3 = PersistableFactory.getMediaTestData().someMinimal();
        media3.setChannel(3);
        media3.setCourtroom(hearing1.getCourtroom());
        media3.setIsCurrent(true);
        dartsPersistence.save(media3);

        hearing1.addMedia(media1);
        hearing1.addMedia(media2);
        hearing1.addMedia(media3);

        hearingRepository.saveAll(List.of(hearing1));

        Pageable sortedByChannelDesc =
            PageRequest.of(0, 3, Sort.by("channel").descending());

        // when
        Page<AdminCaseAudioResponseItem> pages = mediaRepository.findByCaseIdAndIsCurrentTruePageable(hearing1.getCourtCase().getId(), sortedByChannelDesc);

        // then
        assertEquals(3, pages.getTotalElements());

        var results = pages.stream().toList();
        assertThat(results)
            .extracting(AdminCaseAudioResponseItem::getId)
            .containsExactly(media3.getId(), media2.getId(), media1.getId());

        assertThat(results)
            .extracting(AdminCaseAudioResponseItem::getChannel)
            .containsExactly(3, 2, 1);
    }
}