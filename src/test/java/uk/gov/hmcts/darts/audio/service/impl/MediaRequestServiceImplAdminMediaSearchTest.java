package uk.gov.hmcts.darts.audio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideResponse;
import uk.gov.hmcts.darts.audio.validation.MediaHideOrShowValidator;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @AfterEach
    void finish() {
        if (adminMediaSearchResponseMapperMockedStatic != null) {
            adminMediaSearchResponseMapperMockedStatic.close();
        }
    }

    @Test
    void transformedMediaIdNotExist() throws JsonProcessingException {
        Integer transformedMediaId = 1;
        when(mockTransformedMediaRepository.findById(transformedMediaId))
            .thenReturn(Optional.empty());

        List<GetAdminMediaResponseItem> response = mediaRequestService.adminMediaTransformedMediaSearch(transformedMediaId);

        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = """
            [
             ]""";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
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


        List<GetAdminMediaResponseItem> response = mediaRequestService.adminMediaTransformedMediaSearch(transformedMediaId);

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
    void okTwoResponse() throws JsonProcessingException, JsonProcessingException {
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


        List<GetAdminMediaResponseItem> response = mediaRequestService.adminMediaTransformedMediaSearch(transformedMediaId);

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

    @Test
    void testMediaDocumentHide() {
        MediaHideRequest request = new MediaHideRequest();
        request.setIsHidden(true);
        setupTestMediaHide(request);
    }

    @Test
    void testMediaHideDefaultIsHidden() {
        MediaHideRequest request = new MediaHideRequest();
        setupTestMediaHide(request);
    }

    void setupTestMediaHide(MediaHideRequest request) {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        adminMediaSearchResponseMapperMockedStatic = Mockito.mockStatic(GetAdminMediaResponseMapper.class);

        Integer hideOrShowTranscriptionDocument = 343;
        Integer reasonId = 555;

        String ticketReference = "my ticket reference";
        String comments = "my comments";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(reasonId);
        adminActionRequest.setTicketReference(ticketReference);
        adminActionRequest.setComments(comments);

        request.setAdminAction(adminActionRequest);

        UserAccountEntity userAccountEntity = mock(UserAccountEntity.class);

        MediaEntity mediaEntity = new MediaEntity();
        when(mediaRepository.findById(hideOrShowTranscriptionDocument)).thenReturn(Optional.of(mediaEntity));
        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        when(mediaRepository.saveAndFlush(mediaEntityArgumentCaptor.capture())).thenReturn(mediaEntity);
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        ObjectHiddenReasonEntity objectHiddenReasonEntity = new ObjectHiddenReasonEntity();
        MediaHideResponse expectedResponse = new MediaHideResponse();

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(objectHiddenReasonEntity));

        when(objectAdminActionRepository.saveAndFlush(objectAdminActionEntityArgumentCaptor.capture())).thenReturn(objectAdminActionEntity);

        adminMediaSearchResponseMapperMockedStatic.when(
                () -> GetAdminMediaResponseMapper.mapHideOrShowResponse(mediaEntity, objectAdminActionEntity))
            .thenReturn(expectedResponse);


        //run the test
        MediaHideResponse actualResponse
            = mediaRequestService.adminHideOrShowMediaById(hideOrShowTranscriptionDocument, request);


        // make the assertion
        Assertions.assertTrue(mediaEntityArgumentCaptor.getValue().isHidden());
        Assertions.assertEquals(expectedResponse, actualResponse);
        Assertions.assertEquals(request.getAdminAction().getComments(), objectAdminActionEntityArgumentCaptor.getValue().getComments());
        Assertions.assertEquals(request.getAdminAction().getReasonId(), reasonId);
        Assertions.assertFalse(objectAdminActionEntityArgumentCaptor.getValue().isMarkedForManualDeletion());
        Assertions.assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getHiddenBy());
        Assertions.assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getHiddenDateTime());
        Assertions.assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getMarkedForManualDelBy());
        Assertions.assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getMarkedForManualDelDateTime());
    }

    @Test
    void testMediaShow() {
        adminMediaSearchResponseMapperMockedStatic = Mockito.mockStatic(GetAdminMediaResponseMapper.class);

        MediaHideRequest request = new MediaHideRequest();
        request.setIsHidden(false);

        Integer hideOrShowTranscriptionDocument = 343;
        Integer reasonId = 555;

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(reasonId);
        request.setAdminAction(adminActionRequest);

        MediaEntity mediaEntity = new MediaEntity();
        when(mediaRepository.findById(hideOrShowTranscriptionDocument)).thenReturn(Optional.of(mediaEntity));

        Integer objectAdminActionEntityId = 1000;
        Integer objectAdminActionEntityId1 = 1001;

        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setId(objectAdminActionEntityId);
        ObjectAdminActionEntity objectAdminActionEntity1 = new ObjectAdminActionEntity();
        objectAdminActionEntity1.setId(objectAdminActionEntityId1);

        when(mediaRepository.saveAndFlush(mediaEntityArgumentCaptor.capture())).thenReturn(mediaEntity);
        when(objectAdminActionRepository
                 .findByMedia_Id(hideOrShowTranscriptionDocument)).thenReturn(List.of(objectAdminActionEntity, objectAdminActionEntity1));

        MediaHideResponse expectedResponse = new MediaHideResponse();

        adminMediaSearchResponseMapperMockedStatic.when(() -> GetAdminMediaResponseMapper.mapHideOrShowResponse(mediaEntity, null))
            .thenReturn(expectedResponse);


        // run the test
        MediaHideResponse actualResponse
            = mediaRequestService.adminHideOrShowMediaById(hideOrShowTranscriptionDocument, request);

        // make the assertion
        Assertions.assertFalse(mediaEntityArgumentCaptor.getValue().isHidden());
        Assertions.assertEquals(expectedResponse, actualResponse);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId1);
    }
}