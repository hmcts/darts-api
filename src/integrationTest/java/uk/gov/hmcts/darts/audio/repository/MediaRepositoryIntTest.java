package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class MediaRepositoryIntTest extends IntegrationBase {

    public static final LocalDateTime DATE_NOW = DateConverterUtil.toLocalDateTime(OffsetDateTime.now());

    @Autowired
    HearingRepository hearingRepository;
    @Autowired
    CourtCaseStub caseStub;

    @Autowired
    MediaRepository mediaRepository;

    @Autowired
    private MediaStub mediaStub;

    private List<MediaEntity> generatedMediaEntities;

    private static final int GENERATION_COUNT = 20;

    @BeforeEach
    public void before() {
        generatedMediaEntities = mediaStub.generateMediaEntities(GENERATION_COUNT);
    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void testFindMediasByCaseId() {

        // given
        var caseA = caseStub.createAndSaveCourtCaseWithHearings();
        var caseB = caseStub.createAndSaveCourtCaseWithHearings();

        var hearA1 = caseA.getHearings().get(0);
        var hearA2 = caseA.getHearings().get(1);
        var hearA3 = caseA.getHearings().get(2);
        var hearB = caseB.getHearings().get(0);

        var medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        hearA1.addMedia(medias.get(0));
        hearA1.addMedia(medias.get(1));
        hearA2.addMedia(medias.get(2));
        hearB.addMedia(medias.get(0));
        hearingRepository.save(hearA2);
        hearingRepository.save(hearA3);
        hearingRepository.save(hearA1);
        hearingRepository.save(hearB);

        // when
        var caseAMedias = mediaRepository.findAllByCaseId(caseA.getId());
        var caseBMedias = mediaRepository.findAllByCaseId(caseB.getId());

        // then
        var caseAMediasId = caseAMedias.stream().map(MediaEntity::getId);
        assertThat(caseAMediasId).containsExactlyInAnyOrder(medias.get(0).getId(), medias.get(1).getId(), medias.get(2).getId());
        var caseBMediasId = caseBMedias.stream().map(MediaEntity::getId);
        assertThat(caseBMediasId).containsExactlyInAnyOrder(medias.get(0).getId());

        List<CourtCaseEntity> media0cases = medias.get(0).associatedCourtCases();
        assertThat(media0cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId(), caseB.getId());
        List<CourtCaseEntity> media1cases = medias.get(1).associatedCourtCases();
        assertThat(media1cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId());
    }

    @Test
    void testFindAllMediaDetailsWithoutParameters() {
        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(null, null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
    }

    @Test
    void testFindMediaDetailsWithHearingId() {
        MediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            List.of(hearingId), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindMediaDetailsWithHearingIdAndStartDate() {
        MediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());
        OffsetDateTime startTime = expectedMedia.getStart();

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(List.of(hearingId), startTime, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindMediaDetailsWithHearingIdAndStartDateAndEndDate() {
        MediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());
        OffsetDateTime startTime = expectedMedia.getStart();
        OffsetDateTime endTime = expectedMedia.getEnd();
        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(List.of(hearingId), startTime, endTime);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindMediaDetailsWithStartDateAndEndDate() {
        // get all records before record 10 based on the date
        MediaEntity expectedMedia = generatedMediaEntities.get(10);

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
             null, expectedMedia.getStart(), expectedMedia.getEnd());
        Assertions.assertEquals(11, transformedMediaEntityList.size());

        for (int results = 0; results < transformedMediaEntityList.size(); results++) {
            Integer expectedId = generatedMediaEntities.get(results).getId();
            Assertions.assertEquals(expectedId, transformedMediaEntityList.get(results).getId());
        }
    }

    @Test
    void testFindMediaDetailsWithStartDate() {
        MediaEntity expectedMedia = generatedMediaEntities.get(1);

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
             null, expectedMedia.getStart(), null);

        Assertions.assertEquals(2, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(0).getId(), transformedMediaEntityList.get(0).getId());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(1).getId());
    }

    @Test
    void testFindMediaDetailsWithStartDateAndEndDateAndHearingId() {
        MediaEntity expectedMedia = generatedMediaEntities.get(10);

        Integer hearingId = mediaStub.getHearingId(expectedMedia.getId());

        List<MediaEntity> transformedMediaEntityList
            = mediaRepository.findMediaByDetails(
            List.of(hearingId), expectedMedia.getStart(), expectedMedia.getEnd());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
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
        Assertions.assertEquals(2, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
        Assertions.assertEquals(expectedMedia1.getId(), transformedMediaEntityList.get(1).getId());
    }
}