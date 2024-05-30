package uk.gov.hmcts.darts.audio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseItem;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class MediaRequestServiceImplAdminMediaSearchTest {

    @InjectMocks
    private MediaRequestServiceImpl mediaRequestService;

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private TransformedMediaRepository mockTransformedMediaRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = TestUtils.getObjectMapper();
    }

    @Test
    void transformedMediaIdNotExist() {
        Integer transformedMediaId = 1;
        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.empty());

        DartsApiException dartsApiException = assertThrows(DartsApiException.class, () ->
            mediaRequestService.adminMediaSearch(transformedMediaId, null));

        assertEquals("The requested transformed media ID 1 cannot be found", dartsApiException.getDetail());
    }

    @Test
    void okOneResponse() throws JsonProcessingException {
        HearingEntity hearing = createHearing();

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        mediaRequest.setId(8);
        mediaRequest.setHearing(hearing);
        mediaRequest.setStartTime(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        mediaRequest.setEndTime(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));

        TransformedMediaEntity transformedMedia = new TransformedMediaEntity();
        transformedMedia.setId(4);
        transformedMedia.setMediaRequest(mediaRequest);


        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(5);
        mediaEntity.setChannel(6);
        mediaEntity.setStart(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        mediaEntity.setEnd(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));
        Integer transformedMediaId = 1;

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.of(transformedMedia));
        when(mediaRepository.findAllByHearingId(hearing.getId()))
            .thenReturn(List.of(mediaEntity));


        List<AdminMediaSearchResponseItem> response = mediaRequestService.adminMediaSearch(transformedMediaId, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
               {
                 "id": 5,
                 "channel": 6,
                 "start_at": "2020-10-10T10:00:00Z",
                 "end_at": "2020-10-10T11:00:00Z",
                 "hearing": {
                   "id": 3,
                   "hearing_date": "2020-10-10"
                 },
                 "courthouse": {
                   "id": 1,
                   "display_name": "courthouseName1"
                 },
                 "courtroom": {
                   "id": 2,
                   "display_name": "Courtroom1"
                 },
                 "case": {
                   "id": 7,
                   "case_number": "caseNumber1"
                 }
               }
             ]""";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void okTwoResponse() throws JsonProcessingException {
        HearingEntity hearing = createHearing();

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        mediaRequest.setId(8);
        mediaRequest.setHearing(hearing);
        mediaRequest.setStartTime(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        mediaRequest.setEndTime(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));

        TransformedMediaEntity transformedMedia = new TransformedMediaEntity();
        transformedMedia.setId(4);
        transformedMedia.setMediaRequest(mediaRequest);

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(5);
        mediaEntity.setChannel(6);
        mediaEntity.setStart(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        mediaEntity.setEnd(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));

        MediaEntity mediaEntity2 = new MediaEntity();
        mediaEntity2.setId(50);
        mediaEntity2.setChannel(60);
        mediaEntity2.setStart(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        mediaEntity2.setEnd(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));

        Integer transformedMediaId = 1;

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.of(transformedMedia));
        when(mediaRepository.findAllByHearingId(hearing.getId()))
            .thenReturn(List.of(mediaEntity, mediaEntity2));


        List<AdminMediaSearchResponseItem> response = mediaRequestService.adminMediaSearch(transformedMediaId, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
              {
                "id": 5,
                "channel": 6,
                "start_at": "2020-10-10T10:00:00Z",
                "end_at": "2020-10-10T11:00:00Z",
                "hearing": {
                  "id": 3,
                  "hearing_date": "2020-10-10"
                },
                "courthouse": {
                  "id": 1,
                  "display_name": "courthouseName1"
                },
                "courtroom": {
                  "id": 2,
                  "display_name": "Courtroom1"
                },
                "case": {
                  "id": 7,
                  "case_number": "caseNumber1"
                }
              },
              {
                "id": 50,
                "channel": 60,
                "start_at": "2020-10-10T10:00:00Z",
                "end_at": "2020-10-10T11:00:00Z",
                "hearing": {
                  "id": 3,
                  "hearing_date": "2020-10-10"
                },
                "courthouse": {
                  "id": 1,
                  "display_name": "courthouseName1"
                },
                "courtroom": {
                  "id": 2,
                  "display_name": "Courtroom1"
                },
                "case": {
                  "id": 7,
                  "case_number": "caseNumber1"
                }
              }
            ]""";
        JSONAssert.assertEquals(expectedString,
                                responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @NotNull
    private static HearingEntity createHearing() {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setId(1);
        courthouse.setDisplayName("courthouseName1");

        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setId(2);
        courtroom.setName("Courtroom1");
        courtroom.setCourthouse(courthouse);

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setId(7);
        courtCase.setCaseNumber("caseNumber1");

        HearingEntity hearing = new HearingEntity();
        hearing.setId(3);
        hearing.setHearingDate(LocalDate.of(2020, 10, 10));
        hearing.setCourtroom(courtroom);
        hearing.setCourtCase(courtCase);
        return hearing;
    }

}
