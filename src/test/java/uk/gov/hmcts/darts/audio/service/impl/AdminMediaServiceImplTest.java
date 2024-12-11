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
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.MediaSearchData;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMediaServiceImplTest {
    @InjectMocks
    private AdminMediaServiceImpl mediaRequestService;

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private TransformedMediaRepository mockTransformedMediaRepository;

    @Mock
    private SearchMediaValidator searchMediaValidator;

    private ObjectMapper objectMapper;

    private final OffsetDateTime startDateTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    private final OffsetDateTime endDateTime = OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC);

    private static final String MEDIA_ID_5 = """
          {
            "id": 5,
            "channel": 6,
            "start_at": "2020-10-10T10:00:00Z",
            "end_at": "2020-10-10T11:00:00Z",
            "is_hidden": false,
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
              "display_name": "COURTROOM1"
            },
            "case": {
              "id": 7,
              "case_number": "caseNumber1"
            }
          }
        """;

    private static final String MEDIA_ID_50 = """
          {
            "id": 50,
            "channel": 60,
            "start_at": "2020-10-10T10:00:00Z",
            "end_at": "2020-10-10T11:00:00Z",
            "is_hidden": false,
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
              "display_name": "COURTROOM1"
            },
            "case": {
              "id": 7,
              "case_number": "caseNumber1"
            }
          }
        """;

    @BeforeEach
    void setUp() {
        this.objectMapper = TestUtils.getObjectMapper();
    }

    private void disableManualDeletion() {
        this.mediaRequestService = spy(mediaRequestService);
        when(mediaRequestService.isManualDeletionEnabled()).thenReturn(false);
    }

    @Test
    void getMediasMarkedForDeletion_shouldThrowException_whenFeatureNotEnabled() {
        disableManualDeletion();
        DartsApiException dartsApiException = assertThrows(DartsApiException.class, () -> mediaRequestService.getMediasMarkedForDeletion());
        assertThat(dartsApiException.getError()).isEqualTo(CommonApiError.FEATURE_FLAG_NOT_ENABLED);
    }

    @Test
    void adminApproveMediaMarkedForDeletion_shouldThrowException_whenFeatureNotEnabled() {
        disableManualDeletion();
        DartsApiException dartsApiException = assertThrows(DartsApiException.class, () -> mediaRequestService.adminApproveMediaMarkedForDeletion(1));
        assertThat(dartsApiException.getError()).isEqualTo(CommonApiError.FEATURE_FLAG_NOT_ENABLED);
    }

    @Test
    void filterMedias_alwaysValidatesSearchData() {
        mediaRequestService.filterMedias(1, List.of(), startDateTime, endDateTime);
        verify(searchMediaValidator, times(1)).validate(any(MediaSearchData.class));
    }

    @Test
    void filterMedias_shouldReturnEmptyList_whenTransformedMediaIdNotExist() {
        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(1, null, null, null);
        assertEquals(0, response.size());
    }

    @Test
    void filterMedias_shouldReturnSingleMediaRelatedToTransformedMedia_whenTransformedMediaIdGiven() throws JsonProcessingException {
        HearingEntity hearing = createHearing();

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        mediaRequest.setId(8);
        mediaRequest.setHearing(hearing);
        mediaRequest.setStartTime(startDateTime);
        mediaRequest.setEndTime(endDateTime);

        TransformedMediaEntity transformedMedia = new TransformedMediaEntity();
        transformedMedia.setId(4);
        transformedMedia.setMediaRequest(mediaRequest);

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(5);
        mediaEntity.setChannel(6);
        mediaEntity.setStart(startDateTime);
        mediaEntity.setEnd(endDateTime);
        Integer transformedMediaId = 1;

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.of(transformedMedia));
        when(mediaRepository.findAllByHearingId(hearing.getId()))
            .thenReturn(List.of(mediaEntity));

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(transformedMediaId, null, null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = "[" + MEDIA_ID_5 + "]";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void filterMedias_shouldReturnMultipleMediaRelatedToTransformedMedia_whenTransformedMediaIdGiven() throws JsonProcessingException {
        HearingEntity hearing = createHearing();

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        mediaRequest.setId(8);
        mediaRequest.setHearing(hearing);
        mediaRequest.setStartTime(startDateTime);
        mediaRequest.setEndTime(endDateTime);

        TransformedMediaEntity transformedMedia = new TransformedMediaEntity();
        transformedMedia.setId(4);
        transformedMedia.setMediaRequest(mediaRequest);

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(5);
        mediaEntity.setChannel(6);
        mediaEntity.setStart(startDateTime);
        mediaEntity.setEnd(endDateTime);

        MediaEntity mediaEntity2 = new MediaEntity();
        mediaEntity2.setId(50);
        mediaEntity2.setChannel(60);
        mediaEntity2.setStart(startDateTime);
        mediaEntity2.setEnd(endDateTime);

        Integer transformedMediaId = 1;

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.of(transformedMedia));
        when(mediaRepository.findAllByHearingId(hearing.getId()))
            .thenReturn(List.of(mediaEntity, mediaEntity2));

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(transformedMediaId, null, null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = "[" + MEDIA_ID_5 + "," + MEDIA_ID_50 + "]";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void filterMedias_shouldReturnMultipleMediaRelatedToTransformedMedia_whenTransformedMediaIdGivenAndOneMediaIsHidden() throws JsonProcessingException {
        HearingEntity hearing = createHearing();

        MediaRequestEntity mediaRequest = new MediaRequestEntity();
        mediaRequest.setId(8);
        mediaRequest.setHearing(hearing);
        mediaRequest.setStartTime(startDateTime);
        mediaRequest.setEndTime(endDateTime);

        TransformedMediaEntity transformedMedia = new TransformedMediaEntity();
        transformedMedia.setId(4);
        transformedMedia.setMediaRequest(mediaRequest);

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(5);
        mediaEntity.setChannel(6);
        mediaEntity.setStart(startDateTime);
        mediaEntity.setEnd(endDateTime);

        MediaEntity mediaEntity2 = new MediaEntity();
        mediaEntity2.setId(50);
        mediaEntity2.setChannel(60);
        mediaEntity2.setStart(startDateTime);
        mediaEntity2.setEnd(endDateTime);
        mediaEntity2.setHidden(true);

        Integer transformedMediaId = 1;

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.of(transformedMedia));
        when(mediaRepository.findAllByHearingId(hearing.getId()))
            .thenReturn(List.of(mediaEntity, mediaEntity2));

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(transformedMediaId, null, null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = "[" + MEDIA_ID_5 + "," + MEDIA_ID_50.replace("\"is_hidden\": false", "\"is_hidden\": true") + "]";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void filterMedias_shouldReturnSingleMediaForHearings_whenHearingIdGiven() throws Exception {
        HearingEntity hearing = createHearing();

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(5);
        mediaEntity.setChannel(6);
        mediaEntity.setStart(startDateTime);
        mediaEntity.setEnd(endDateTime);

        mediaEntity.getHearingList().add(hearing);

        when(mediaRepository.findMediaByDetails(List.of(hearing.getId()), null, null))
            .thenReturn(List.of(mediaEntity));

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(null, List.of(hearing.getId()), null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = "[" + MEDIA_ID_5 + "]";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void filterMedias_shouldReturnSingleMediaForHearingsAndStartEndTimes_whenMediaIsAssociatedWithMultipleHearings() throws JsonProcessingException {
        HearingEntity hearing = createHearing();
        HearingEntity hearing2 = createHearing();

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(5);
        mediaEntity.setChannel(6);
        mediaEntity.setStart(startDateTime);
        mediaEntity.setEnd(endDateTime);
        mediaEntity.getHearingList().addAll(List.of(hearing, hearing2));

        when(mediaRepository.findMediaByDetails(List.of(hearing.getId(), hearing2.getId()), startDateTime, endDateTime))
            .thenReturn(List.of(mediaEntity));

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(null,
                                                                                    List.of(hearing.getId(), hearing2.getId()),
                                                                                    startDateTime, endDateTime);

        assertEquals(1, response.size());
        assertEquals(mediaEntity.getId(), response.get(0).getId());
        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = "[" + MEDIA_ID_5 + "]";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
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