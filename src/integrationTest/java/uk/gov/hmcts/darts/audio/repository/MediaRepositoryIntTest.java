package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    HearingRepository hearingRepository;

    @Autowired
    CourtCaseStub caseStub;

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

        // then
        var caseAMediasId = caseAMedias.stream().map(MediaEntity::getId);
        assertThat(caseAMediasId).containsExactlyInAnyOrder(media0.getId(), media1.getId(), media2.getId());
        var caseBMediasId = caseBMedias.stream().map(MediaEntity::getId);
        assertThat(caseBMediasId).containsExactlyInAnyOrder(media0.getId());

        List<CourtCaseEntity> media0cases = media0.associatedCourtCases();
        assertThat(media0cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId(), caseB.getId());
        List<CourtCaseEntity> media1cases = media1.associatedCourtCases();
        assertThat(media1cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId());

    }

    @Test
    void testFindAllMediaDetailsWithoutParameters() {
        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(null, null, null);
        assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
    }

    @Test
    void testFindMediaDetailsWithHearingId() {
        MediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            List.of(hearingId), null, null);
        assertEquals(1, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindMediaDetailsWithHearingIdAndStartDate() {
        MediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());
        OffsetDateTime startTime = expectedMedia.getStart();

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(List.of(hearingId), startTime, null);
        assertEquals(1, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindMediaDetailsWithHearingIdAndStartDateAndEndDate() {
        MediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());
        OffsetDateTime startTime = expectedMedia.getStart();
        OffsetDateTime endTime = expectedMedia.getEnd();
        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(List.of(hearingId), startTime, endTime);
        assertEquals(1, transformedMediaEntityList.size());
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
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
            Integer expectedId = generatedMediaEntities.get(results).getId();
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
        assertEquals(generatedMediaEntities.get(0).getId(), transformedMediaEntityList.get(0).getId());
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
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
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
        assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
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

    private MediaLinkedCaseEntity createMediaLinkedCase(MediaEntity media, CourtCaseEntity courtCase) {
        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setMedia(media);
        mediaLinkedCase.setCourtCase(courtCase);
        return mediaLinkedCase;
    }
}