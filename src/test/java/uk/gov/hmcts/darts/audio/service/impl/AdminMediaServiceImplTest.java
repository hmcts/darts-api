package uk.gov.hmcts.darts.audio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.mapper.GetAdminMediaResponseMapper;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.validation.MediaHideOrShowValidator;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private MediaHideOrShowValidator mediaHideOrShowValidator;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private SearchMediaValidator searchMediaValidator;


    @Captor
    ArgumentCaptor<ObjectAdminActionEntity> objectAdminActionEntityArgumentCaptor;

    @Captor
    ArgumentCaptor<MediaEntity> mediaEntityArgumentCaptor;

    private ObjectMapper objectMapper;

    private MockedStatic<GetAdminMediaResponseMapper> adminMediaSearchResponseMapperMockedStatic;

    @BeforeEach
    void setUp() {
        this.objectMapper = TestUtils.getObjectMapper();
    }

    private void disableManualDeletion() {
        this.mediaRequestService = spy(mediaRequestService);
        when(mediaRequestService.isManualDeletionEnabled()).thenReturn(false);
    }

    @Test
    void getMediasMarkedForDeletionManualDeletionDisabled() {
        disableManualDeletion();
        DartsApiException dartsApiException = assertThrows(DartsApiException.class, () -> mediaRequestService.getMediasMarkedForDeletion());
        assertThat(dartsApiException.getError()).isEqualTo(CommonApiError.FEATURE_FLAG_NOT_ENABLED);
    }

    @Test
    void adminApproveMediaMarkedForDeletionManualDeletionDisabled() {
        disableManualDeletion();
        DartsApiException dartsApiException = assertThrows(DartsApiException.class, () -> mediaRequestService.adminApproveMediaMarkedForDeletion(1));
        assertThat(dartsApiException.getError()).isEqualTo(CommonApiError.FEATURE_FLAG_NOT_ENABLED);
    }

    @Test
    void transformedMediaIdNotExist() throws JsonProcessingException {
        Integer transformedMediaId = 1;
        List<Integer> hearingIds = List.of();
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = OffsetDateTime.now();

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(transformedMediaId, hearingIds, startAt, endAt);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
             ]""";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
        verify(searchMediaValidator, times(1)).validate(Mockito.notNull());
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

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(transformedMediaId, null, null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
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
            ]""";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
        verify(searchMediaValidator, times(1)).validate(Mockito.notNull());
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

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(transformedMediaId, null, null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
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
              },
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
            ]""";
        JSONAssert.assertEquals(expectedString,
                                responseString, JSONCompareMode.NON_EXTENSIBLE);

        verify(searchMediaValidator, times(1)).validate(Mockito.notNull());
    }

    @Test
    void okTwoResponseOneHidden() throws JsonProcessingException {
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
        mediaEntity2.setHidden(true);

        Integer transformedMediaId = 1;

        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.of(transformedMedia));
        when(mediaRepository.findAllByHearingId(hearing.getId()))
            .thenReturn(List.of(mediaEntity, mediaEntity2));

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(transformedMediaId, null, null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
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
              },
              {
                "id": 50,
                "channel": 60,
                "start_at": "2020-10-10T10:00:00Z",
                "end_at": "2020-10-10T11:00:00Z",
                "is_hidden": true,
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
            ]""";
        JSONAssert.assertEquals(expectedString,
                                responseString, JSONCompareMode.NON_EXTENSIBLE);

        verify(searchMediaValidator, times(1)).validate(Mockito.notNull());
    }

    @Test
    void testFilterMediasByHearingId() throws Exception {
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

        mediaEntity.getHearingList().add(hearing);

        when(mediaRepository.findMediaByDetails(List.of(hearing.getId()), null, null))
            .thenReturn(List.of(mediaEntity));

        List<GetAdminMediaResponseItem> response = mediaRequestService.filterMedias(null, List.of(hearing.getId()), null, null);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
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
            ]""";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
        verify(searchMediaValidator, times(1)).validate(Mockito.notNull());
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