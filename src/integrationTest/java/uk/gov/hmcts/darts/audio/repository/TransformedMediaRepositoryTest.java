package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.testutils.RepositoryBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;
import uk.gov.hmcts.darts.testutils.stubs.TransformedMediaStub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

class TransformedMediaRepositoryTest extends RepositoryBase {

    @Autowired
    private TransformedMediaStub transformedMediaStub;

    @Autowired
    private MediaRequestStub mediaRequestStub;

    @Autowired
    private TransformedMediaRepository transformedMediaRepository;

    private List<TransformedMediaEntity> generatedMediaEntities;

    private static final int GENERATION_COUNT = 20;


    @BeforeEach
    public void before() {
        generatedMediaEntities = transformedMediaStub.generateTransformedMediaEntities(GENERATION_COUNT);
    }

    @Test
    void testFindTransformedMediaWithoutAnyParameters() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null,
                null, null, null, null, null);
        Assertions.assertEquals(generatedMediaEntities.size(), transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub.getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithId() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(generatedMediaEntities.get(0).getId(), null, null, null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(0).getId(), transformedMediaEntityList.size());
    }

    @Test
    void testFindTransformedMediaWithCaseNumber() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, generatedMediaEntities.get(3).getMediaRequest().getHearing().getCourtCase().getCaseNumber(), null, null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(transformedMediaEntityList.get(0).getId(), generatedMediaEntities.get(3).getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameSubstringPrefixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null,
                TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(Integer.toString(nameMatchIndex)), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameCaseInsensitiveSubstringPrefixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null,
            TransformedMediaSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameSubstringPostFixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null,
                TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameCaseInsensitiveSubstringPostFixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null,
            TransformedMediaSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPostfix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameSubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null,
                TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub.getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithHearingDate() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null,
                generatedMediaEntities.get(3).getMediaRequest().getHearing().getHearingDate(), null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(3).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerExact() {
        int nameMatchIndex = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null, null,
                TransformedMediaSubStringQueryEnum.OWNER.getQueryString(Integer.toString(nameMatchIndex)), null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerPrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null, null,
                TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null);

        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerCaseInsensitivePrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER
                .getQueryStringPostfix(Integer.toString(nameMatchIndex))
                .toLowerCase(Locale.getDefault()), null, null, null);

        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerPostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null);
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerCaseInsensitivePostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER
                .getQueryStringPostfix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null);
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerSubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null,  null, null,
                TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(), null, null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithRequestedByExact() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null, null, null,
                TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(nameMatchIndex)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByPrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null, null, null,
                TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer.toString(13)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByCaseInsensitivePrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer.toString(13)).toUpperCase(Locale.getDefault()), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByPostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(13)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByCaseInsensitivePostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(13)).toLowerCase(Locale.getDefault()), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedBySubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null,  null, null, null,
                TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(), null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithRequestedAtFromAndRequestedAtToSameDay() {
        int fromAtPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null, null, null, null,
                generatedMediaEntities.get(fromAtPosition).getMediaRequest().getCreatedDateTime(),
                generatedMediaEntities.get(fromAtPosition).getMediaRequest().getCreatedDateTime());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities
                                    .get(fromAtPosition).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedAtFrom() {
        int fromAtPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
                null, null, null, null, null,
                null, generatedMediaEntities.get(fromAtPosition).getMediaRequest().getCreatedDateTime(), null);
        Assertions.assertEquals(GENERATION_COUNT - fromAtPosition, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getExpectedStartingFrom(fromAtPosition, generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithRequestedAtTo() {
        int toPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null,
                                                              null, null, null, null, null, generatedMediaEntities.get(toPosition).getCreatedDateTime());
        Assertions.assertEquals(toPosition + 1, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getExpectedTo(toPosition, generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithAllQueryParameters() {
        TransformedMediaEntity transformedMediaEntityFind = generatedMediaEntities.get(1);
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(transformedMediaEntityFind.getId(), transformedMediaEntityFind.getMediaRequest()
            .getHearing().getCourtCase().getCaseNumber(), transformedMediaEntityFind.getMediaRequest()
            .getHearing().getCourtroom().getCourthouse().getDisplayName(), transformedMediaEntityFind.getMediaRequest()
            .getHearing().getHearingDate(), transformedMediaEntityFind.getMediaRequest()
            .getCurrentOwner().getUserFullName(), transformedMediaEntityFind.getCreatedBy().getUserFullName(), transformedMediaEntityFind.getMediaRequest()
            .getCreatedBy().getCreatedDateTime(), generatedMediaEntities.get(1).getCreatedDateTime());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(transformedMediaEntityFind.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaDateRangeWithTransformedIds() {
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(0);

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            generatedMediaEntities.get(0).getId(), null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaDateRangeWithTransformedIdsAndHearingId() {
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = expectedMedia.getMediaRequest().getHearing().getId();

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            generatedMediaEntities.get(0).getId(), List.of(hearingId), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaDateRangeWithTransformedIdsAndHearingIdAndStartDate() {
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = expectedMedia.getMediaRequest().getHearing().getId();
        OffsetDateTime startTime = expectedMedia.getMediaRequest().getStartTime();

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            generatedMediaEntities.get(0).getId(), List.of(hearingId), startTime, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaDateRangeWithTransformedIdsAndHearingIdAndStartDateAndEndDate() {
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(0);
        Integer hearingId = expectedMedia.getMediaRequest().getHearing().getId();
        OffsetDateTime startTime = expectedMedia.getMediaRequest().getStartTime();
        OffsetDateTime endTime = expectedMedia.getMediaRequest().getEndTime();

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            generatedMediaEntities.get(0).getId(), List.of(hearingId), startTime, endTime);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaDateRangeWithStartDateAndEndDate() {
        // get all records before record 10 based on the date
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(10);

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            null, null, expectedMedia.getStartTime(), expectedMedia.getEndTime());
        Assertions.assertEquals(11, transformedMediaEntityList.size());

        for (int results = 0; results < transformedMediaEntityList.size(); results++) {
            Integer expectedId = generatedMediaEntities.get(results).getId();
            Assertions.assertEquals(expectedId, transformedMediaEntityList.get(results).getId());
        }
    }

    @Test
    void testFindTransformedMediaDateRangeWithStartDate() {
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(1);

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            null, null, expectedMedia.getStartTime(), null);

        Assertions.assertEquals(2, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(0).getId(), transformedMediaEntityList.get(0).getId());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(1).getId());
    }

    @Test
    void testFindTransformedMediaDateRangeWithStartDateAndEndDateAndHearingId() {
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(10);

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            null, List.of(expectedMedia.getMediaRequest().getHearing().getId()), expectedMedia.getStartTime(), expectedMedia.getEndTime());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    void testFindTransformedMediaDateRangeWithMultipleHearingIds() {
        TransformedMediaEntity expectedMedia = generatedMediaEntities.get(10);
        TransformedMediaEntity expectedMedia1 = generatedMediaEntities.get(11);

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformMediaWithStartAndEndDateTimeRange(
            null, List.of(expectedMedia.getMediaRequest().getHearing().getId(), expectedMedia1.getMediaRequest().getHearing().getId()), null, null);
        Assertions.assertEquals(2, transformedMediaEntityList.size());
        Assertions.assertEquals(expectedMedia.getId(), transformedMediaEntityList.get(0).getId());
        Assertions.assertEquals(expectedMedia1.getId(), transformedMediaEntityList.get(1).getId());
    }
}