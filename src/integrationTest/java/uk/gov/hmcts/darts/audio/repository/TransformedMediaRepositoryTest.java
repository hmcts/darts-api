package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;
import uk.gov.hmcts.darts.testutils.stubs.TransformedMediaStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransformedMediaRepositoryTest extends IntegrationBase {

    @Autowired
    private TransformedMediaStub transformedMediaStub;

    @Autowired
    private MediaRequestStub mediaRequestStub;

    @Autowired
    private TransformedMediaRepository transformedMediaRepository;

    private List<TransformedMediaEntity> generatedMediaEntities;

    private final static int GENERATION_COUNT = 20;

    private final static String FILE_NAME_PREFIX = "FileName";

    private final static String CASE_NUMBER_PREFIX = "CaseNumber";

    @BeforeEach
    public void before() {
        generatedMediaEntities = generateTestData(GENERATION_COUNT);
    }

    @Test
    public void testFindTransformedMediaWithoutAnyParameters() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, null, null, null, null);
        Assertions.assertEquals(generatedMediaEntities.size(), transformedMediaEntityList.size());
        Assertions.assertTrue(getTransformedMediaIds(transformedMediaEntityList).containsAll(getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    public void testFindTransformedMediaWithId() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(generatedMediaEntities.get(0).getId(), null, null, null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(0).getId(), transformedMediaEntityList.size());
    }

    @Test
    public void testFindTransformedMediaWithCaseNumber() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, generatedMediaEntities.get(3).getMediaRequest().getHearing().getCourtCase().getCaseNumber(), null, null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(transformedMediaEntityList.get(0).getId(), generatedMediaEntities.get(3).getId());
    }

    @Test
    public void testFindTransformedMediaWithCourtDisplayNameSubstringPrefixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(Integer.toString(nameMatchIndex)), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithCourtDisplayNameSubstringPostFixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithCourtDisplayNameSubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null,  TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(getTransformedMediaIds(transformedMediaEntityList).containsAll(getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    public void testFindTransformedMediaWithHearingDate() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, generatedMediaEntities.get(3).getMediaRequest().getHearing().getHearingDate(), null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(3).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithOwnerExact() {
        int nameMatchIndex = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, TransformedMediaSubStringQueryEnum.OWNER.getQueryString(Integer.toString(nameMatchIndex)), null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithOwnerPrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null);

        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithOwnerPostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null);
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithOwnerSubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null,  null, null, TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(), null, null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(getTransformedMediaIds(transformedMediaEntityList).containsAll(getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    public void testFindTransformedMediaWithRequestedByExact() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, null, TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(nameMatchIndex)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithRequestedByPrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, null, TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer.toString(13)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithRequestedByPostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, null, TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(13)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithRequestedBySubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null,  null, null, null, TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(), null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(getTransformedMediaIds(transformedMediaEntityList).containsAll(getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    public void testFindTransformedMediaWithRequestedAtFromAndRequestedAtToSameDay() {
        int fromAtPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, null, null, generatedMediaEntities.get(fromAtPosition).getCreatedDateTime(), generatedMediaEntities.get(fromAtPosition).getCreatedDateTime());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(fromAtPosition).getId(), transformedMediaEntityList.get(0).getId());
    }

    @Test
    public void testFindTransformedMediaWithRequestedAtFrom() {
        int fromAtPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, null, null, generatedMediaEntities.get(fromAtPosition).getCreatedDateTime(), null);
        Assertions.assertEquals(GENERATION_COUNT - fromAtPosition, transformedMediaEntityList.size());
        Assertions.assertTrue(getTransformedMediaIds(transformedMediaEntityList).containsAll(getExpectedStartingFrom(fromAtPosition)));
    }

    @Test
    public void testFindTransformedMediaWithRequestedAtTo() {
        int toPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null, null, null, null, null, null, generatedMediaEntities.get(toPosition).getCreatedDateTime());
        Assertions.assertEquals(toPosition + 1, transformedMediaEntityList.size());
        Assertions.assertTrue(getTransformedMediaIds(transformedMediaEntityList).containsAll(getExpectedTo(toPosition)));
    }

    @Test
    public void testFindTransformedMediaWithAllQueryParameters() {
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

    /**
     * generates test data. The following will be used for generation:-
     * Unique owner and requested by users for each transformed media record
     * Unique court house with unique name for each transformed media record
     * Unique case number with unique case number for each transformed media record
     * Unique hearing date starting with today with an incrementing day for each transformed media record
     * Unique requested date with an incrementing hour for each transformed media record
     * Unique file name with unique name for each transformed media record
     * @param count The number of transformed media objects that are to be generated
     * @return The list of generated media entities in chronological order
     */
    private List<TransformedMediaEntity> generateTestData(int count) {
        List<TransformedMediaEntity> retTransformerMediaLst = new ArrayList<>();
        OffsetDateTime hoursBefore = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime hoursAfter = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime requestedDate = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDateTime hearingDate = LocalDateTime.now(ZoneOffset.UTC);

        int fileSize = 1;
        for (int transformedMediaCount = 0; transformedMediaCount < count; transformedMediaCount++) {
            UserAccountEntity owner = dartsDatabase.getUserAccountStub().createSystemUserAccount(
                TransformedMediaSubStringQueryEnum.OWNER.getQueryString(Integer.toString(transformedMediaCount)));
            UserAccountEntity requestedBy = dartsDatabase.getUserAccountStub().createSystemUserAccount(
                TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(transformedMediaCount)));
            TransformedMediaStub transformedMediaStub = dartsDatabase.getTransformedMediaStub();

            String courtName = TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryString(Integer.toString(transformedMediaCount));
            String caseNumber = CASE_NUMBER_PREFIX + transformedMediaCount;
            String fileName = FILE_NAME_PREFIX + transformedMediaCount + ".txt";
            AudioRequestOutputFormat fileFormat = AudioRequestOutputFormat.ZIP;

            var mediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                owner,
                requestedBy,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.COMPLETED,
                courtName,
                caseNumber,
                hearingDate,
                hoursBefore,
                hoursAfter,
                requestedDate
            );

            retTransformerMediaLst.add(transformedMediaStub.createTransformedMediaEntity(mediaRequest, fileName, null, null, fileFormat, fileSize));
            fileSize = fileSize + 1;
            hoursBefore = hoursBefore.minusHours(1);
            hoursAfter = hoursAfter.plusHours(1);
            hearingDate = hearingDate.plusDays(count);
            requestedDate = requestedDate.plusDays(1);

        }
        return retTransformerMediaLst;
    }

    private List<Integer> getExpectedStartingFrom(int startingFromIndex) {
        List<Integer> fndMediaIds = new ArrayList<>();
        for (int position = 0; position < generatedMediaEntities.size(); position++) {
            if (position >= startingFromIndex) {
                fndMediaIds.add(generatedMediaEntities.get(position).getId());
            }
        }

        return fndMediaIds;
    }

    private List<Integer> getExpectedTo(int toIndex) {
        List<Integer> fndMediaIds = new ArrayList<>();
        for (int position = 0; position < generatedMediaEntities.size(); position++) {
            if (position <= toIndex) {
                fndMediaIds.add(generatedMediaEntities.get(position).getId());
            }
        }

        return fndMediaIds;
    }

    private List<Integer> getTransformedMediaIds(List<TransformedMediaEntity> entities) {
        return entities.stream().map(e -> e.getId()).collect(Collectors.toList());
    }
}