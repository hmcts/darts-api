package uk.gov.hmcts.darts.audio.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

class GetTransformedMediaDetailsMapperTest {

    @Test
    void testMapSearchResults() {

        TransformedMediaEntity entityToMap = getStubbedTransformedMediaEntity();

        // run the tests
        GetTransformedMediaDetailsMapper transformedMediaDetailsMapper = new GetTransformedMediaDetailsMapper();
        List<SearchTransformedMediaResponse> searchResults = transformedMediaDetailsMapper.mapSearchResults(
            new ArrayList<>(List.of(entityToMap)));

        // make the assertions
        Assertions.assertEquals(1, searchResults.size());
        SearchTransformedMediaResponse response = searchResults.get(0);
        Assertions.assertEquals(entityToMap.getId(), response.getId());
        Assertions.assertEquals(entityToMap.getOutputFilesize(), response.getFileSizeBytes());
        Assertions.assertEquals(entityToMap.getLastAccessed(), response.getLastAccessedAt());
        Assertions.assertEquals(AudioRequestOutputFormat.ZIP.name(), response.getFileFormat());
        Assertions.assertEquals(entityToMap.getOutputFilename(), response.getFileName());
        Assertions.assertEquals(entityToMap.getMediaRequest().getId(), response.getMediaRequest().getId());
        Assertions.assertEquals(entityToMap.getCreatedDateTime(), response.getMediaRequest().getRequestedAt());
        Assertions.assertEquals(entityToMap.getMediaRequest().getRequestor().getId(), response.getMediaRequest().getRequestedByUserId());
        Assertions.assertEquals(entityToMap.getMediaRequest().getCurrentOwner().getId(), response.getMediaRequest().getOwnerUserId());
        Assertions.assertEquals(entityToMap.getMediaRequest().getHearing().getCourtCase().getId(), response.getCase().getId());
        Assertions.assertEquals(entityToMap.getMediaRequest().getHearing().getCourtCase().getCaseNumber(), response.getCase().getCaseNumber());
        Assertions.assertEquals(entityToMap.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(), response.getCourthouse().getId());
        Assertions.assertEquals(entityToMap.getMediaRequest().getHearing().getCourtroom().getCourthouse().getDisplayName(),
                                response.getCourthouse().getDisplayName());
        Assertions.assertEquals(entityToMap.getMediaRequest().getHearing().getId(), response.getHearing().getId());
        Assertions.assertEquals(entityToMap.getMediaRequest().getHearing().getHearingDate(), response.getHearing().getHearingDate());
    }

    @Test
    void testMapSearchResultsDefensiveCheckWithCoreEntityDataMissing() {

        // setup the test data
        Integer id = 200;

        TransformedMediaEntity mediaEntity = new TransformedMediaEntity();
        mediaEntity.setId(id);

        // run the tests
        GetTransformedMediaDetailsMapper transformedMediaDetailsMapper = new GetTransformedMediaDetailsMapper();
        List<SearchTransformedMediaResponse> searchResults = transformedMediaDetailsMapper.mapSearchResults(new ArrayList<>(List.of(mediaEntity)));

        // make the assertions
        Assertions.assertEquals(1, searchResults.size());
        SearchTransformedMediaResponse response = searchResults.get(0);
        Assertions.assertEquals(id, response.getId());
        Assertions.assertNull(response.getCourthouse());
        Assertions.assertNull(response.getMediaRequest());
        Assertions.assertNull(response.getCase());
    }

    private TransformedMediaEntity getStubbedTransformedMediaEntity() {
        // setup the test data
        Integer id = 200;
        Integer sizeOfBytes = 3_000_000;
        String fileName = "Filename";
        OffsetDateTime created = OffsetDateTime.now();
        OffsetDateTime lastAccessed = OffsetDateTime.now().plusMonths(2);

        TransformedMediaEntity mediaEntity = new TransformedMediaEntity();
        mediaEntity.setOutputFormat(AudioRequestOutputFormat.ZIP);
        mediaEntity.setOutputFilesize(sizeOfBytes);
        mediaEntity.setOutputFilename(fileName);
        mediaEntity.setCreatedDateTime(created);
        mediaEntity.setLastAccessed(lastAccessed);
        mediaEntity.setId(id);

        String ownerName = "owner name";
        Integer ownerUserId = 7000;
        UserAccountEntity userAccountEntityOwner = new UserAccountEntity();
        userAccountEntityOwner.setUserFullName(ownerName);
        userAccountEntityOwner.setUserName(ownerName);
        userAccountEntityOwner.setId(ownerUserId);

        String requestedBy = "requested by";
        Integer requestedUserId = 4000;
        UserAccountEntity userAccountEntityRequestedBy = new UserAccountEntity();
        userAccountEntityRequestedBy.setUserFullName(requestedBy);
        userAccountEntityRequestedBy.setUserName(requestedBy);
        userAccountEntityRequestedBy.setId(requestedUserId);

        Integer mediaId = 300;

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setId(mediaId);
        mediaRequestEntity.setCurrentOwner(userAccountEntityOwner);
        mediaRequestEntity.setRequestor(userAccountEntityRequestedBy);
        mediaRequestEntity.setCreatedBy(userAccountEntityOwner);
        mediaRequestEntity.setCreatedDateTime(created);

        CourtroomEntity courtroomEntity = new CourtroomEntity();

        String courtHouseDisplayName = "court house display name";
        Integer courtHouseId = 500;
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setDisplayName(courtHouseDisplayName);
        courthouseEntity.setId(courtHouseId);

        courtroomEntity.setCourthouse(courthouseEntity);

        String caseNumber = "my case number";
        Integer caseId = 900;
        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber(caseNumber);
        caseEntity.setId(caseId);

        Integer hearingId = 888;
        LocalDate hearingDate = LocalDate.now().plusMonths(2);

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setId(hearingId);
        hearingEntity.setHearingDate(hearingDate);

        mediaRequestEntity.setHearing(hearingEntity);
        mediaEntity.setMediaRequest(mediaRequestEntity);

        return mediaEntity;
    }
}