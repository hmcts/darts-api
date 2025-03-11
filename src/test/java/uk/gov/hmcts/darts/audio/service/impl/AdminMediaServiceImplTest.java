package uk.gov.hmcts.darts.audio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.audio.component.impl.ApplyAdminActionComponent;
import uk.gov.hmcts.darts.audio.component.impl.RemoveAdminActionComponent;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.mapper.AdminMarkedForDeletionMapper;
import uk.gov.hmcts.darts.audio.mapper.AdminMarkedForDeletionMapperImpl;
import uk.gov.hmcts.darts.audio.mapper.CourthouseMapper;
import uk.gov.hmcts.darts.audio.mapper.CourthouseMapperImpl;
import uk.gov.hmcts.darts.audio.mapper.CourtroomMapper;
import uk.gov.hmcts.darts.audio.mapper.CourtroomMapperImpl;
import uk.gov.hmcts.darts.audio.mapper.GetAdminMediaResponseMapper;
import uk.gov.hmcts.darts.audio.mapper.ObjectActionMapper;
import uk.gov.hmcts.darts.audio.mapper.ObjectActionMapperImpl;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourthouseResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourtroomResponse;
import uk.gov.hmcts.darts.audio.model.AdminVersionedMediaResponse;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionAdminAction;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionMediaItem;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.audio.model.MediaSearchData;
import uk.gov.hmcts.darts.audio.model.PatchAdminMediasByIdRequest;
import uk.gov.hmcts.darts.audio.validation.MediaHideOrShowValidator;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.CouplingBetweenObjects")
@ExtendWith(MockitoExtension.class)
class AdminMediaServiceImplTest {
    @InjectMocks
    @Spy
    private AdminMediaServiceImpl mediaRequestService;

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;
    @Mock
    private TransformedMediaRepository mockTransformedMediaRepository;
    @Mock
    private SearchMediaValidator searchMediaValidator;
    @Mock
    private MediaHideOrShowValidator mediaHideOrShowValidator;
    @Mock
    private ApplyAdminActionComponent applyAdminActionComponent;
    @Mock
    private RemoveAdminActionComponent removeAdminActionComponent;
    @Mock
    private ObjectHiddenReasonRepository hiddenReasonRepository;
    @Mock
    private GetAdminMediaResponseMapper getAdminMediaResponseMapper;

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
        when(mediaRequestService.isManualDeletionEnabled()).thenReturn(false);
    }

    private void enableManualDeletion() {
        when(mediaRequestService.isManualDeletionEnabled()).thenReturn(true);
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


    @Nested
    class GetMediasMarkedForDeletion {
        private static final int OBJECT_ADMIN_ACTION_ENTITY_ID_1 = 4;
        private static final int OBJECT_ADMIN_ACTION_ENTITY_ID_2 = 8;
        private static final int OBJECT_ADMIN_ACTION_ENTITY_ID_3 = 12;
        private ObjectAdminActionEntity objectAdminActionEntity1;
        private ObjectAdminActionEntity objectAdminActionEntity2;
        private ObjectAdminActionEntity objectAdminActionEntity3;

        @BeforeEach
        void beforeEach() {
            enableManualDeletion();
        }

        record ExpectedResult(int... mediaIds) {

        }

        private void triggerAndValidate(List<ObjectAdminActionEntity> objectAdminActionEntity,
                                        ExpectedResult... expectedResults) {

            when(objectAdminActionRepository.findAllMediaActionsWithAnyDeletionReason()).thenReturn(objectAdminActionEntity);

            GetAdminMediasMarkedForDeletionItem responseItem1 = mock(GetAdminMediasMarkedForDeletionItem.class);
            List<GetAdminMediasMarkedForDeletionItem> responseItems = new ArrayList<>();
            responseItems.add(responseItem1);
            if (expectedResults.length > 1) {
                GetAdminMediasMarkedForDeletionItem[] responseItemsArray = new GetAdminMediasMarkedForDeletionItem[expectedResults.length - 1];
                for (int i = 0; i < expectedResults.length - 1; i++) {
                    responseItemsArray[i] = mock(GetAdminMediasMarkedForDeletionItem.class);
                }
                doReturn(responseItem1, (Object[]) responseItemsArray).when(mediaRequestService).toGetAdminMediasMarkedForDeletionItem(any());
            } else {
                doReturn(responseItem1).when(mediaRequestService).toGetAdminMediasMarkedForDeletionItem(any());
            }


            List<GetAdminMediasMarkedForDeletionItem> result = mediaRequestService.getMediasMarkedForDeletion();
            assertThat(result).hasSize(expectedResults.length);
            assertThat(result).containsAll(responseItems);

            ArgumentCaptor<List<ObjectAdminActionEntity>> actionsCaptor = ArgumentCaptor.captor();
            verify(mediaRequestService, times(expectedResults.length)).toGetAdminMediasMarkedForDeletionItem(actionsCaptor.capture());

            List<List<ObjectAdminActionEntity>> actionEntities = actionsCaptor.getAllValues();

            assertThat(actionEntities).hasSize(expectedResults.length);

            //Sort to prevent flaky tests
            actionEntities.sort((a, b) -> b.size() - a.size());

            int expectedResultIndex = 0;
            for (ExpectedResult expectedResult : expectedResults) {
                List<ObjectAdminActionEntity> actionEntityList = actionEntities.get(expectedResultIndex++);
                assertThat(actionEntityList).hasSize(expectedResult.mediaIds().length);
                for (int i = 0; i < expectedResult.mediaIds().length; i++) {
                    assertThat(actionEntityList.get(i).getId()).isEqualTo(expectedResult.mediaIds()[i]);
                }
            }
        }

        private void setupStandingData() {
            OffsetDateTime start = OffsetDateTime.now();
            OffsetDateTime end = OffsetDateTime.now().plusDays(1);
            objectAdminActionEntity1 =
                createObjectAdminActionEntity(
                    "TicketRef1",
                    1,
                    2,
                    3,
                    start,
                    end,
                    OBJECT_ADMIN_ACTION_ENTITY_ID_1
                );
            objectAdminActionEntity2 =
                createObjectAdminActionEntity(
                    "TicketRef1",
                    1,
                    2,
                    3,
                    start,
                    end,
                    OBJECT_ADMIN_ACTION_ENTITY_ID_2
                );
            objectAdminActionEntity3 =
                createObjectAdminActionEntity(
                    "TicketRef1",
                    1,
                    2,
                    3,
                    start,
                    end,
                    OBJECT_ADMIN_ACTION_ENTITY_ID_3
                );
        }

        @Test
        void getMediasMarkedForDeletion_typical() {
            setupStandingData();
            triggerAndValidate(List.of(objectAdminActionEntity1, objectAdminActionEntity2, objectAdminActionEntity3),
                               new ExpectedResult(OBJECT_ADMIN_ACTION_ENTITY_ID_1, OBJECT_ADMIN_ACTION_ENTITY_ID_2, OBJECT_ADMIN_ACTION_ENTITY_ID_3));
        }


        @Test
        void getMediasMarkedForDeletion_shouldGroupByTicketReference() {
            setupStandingData();
            when(objectAdminActionEntity2.getTicketReference()).thenReturn("TicketRef2");
            triggerAndValidate(List.of(objectAdminActionEntity1, objectAdminActionEntity2, objectAdminActionEntity3),
                               new ExpectedResult(OBJECT_ADMIN_ACTION_ENTITY_ID_1, OBJECT_ADMIN_ACTION_ENTITY_ID_3), new ExpectedResult(
                    OBJECT_ADMIN_ACTION_ENTITY_ID_2));
        }

        @Test
        void getMediasMarkedForDeletion_shouldGroupByHiddenById() {
            setupStandingData();
            when(objectAdminActionEntity2.getHiddenBy().getId()).thenReturn(123);
            triggerAndValidate(List.of(objectAdminActionEntity1, objectAdminActionEntity2, objectAdminActionEntity3),
                               new ExpectedResult(OBJECT_ADMIN_ACTION_ENTITY_ID_1, OBJECT_ADMIN_ACTION_ENTITY_ID_3), new ExpectedResult(
                    OBJECT_ADMIN_ACTION_ENTITY_ID_2));
        }

        @Test
        void getMediasMarkedForDeletion_shouldGroupByObjectHiddenReasons() {
            setupStandingData();
            when(objectAdminActionEntity2.getObjectHiddenReason().getId()).thenReturn(123);
            triggerAndValidate(List.of(objectAdminActionEntity1, objectAdminActionEntity2, objectAdminActionEntity3),
                               new ExpectedResult(OBJECT_ADMIN_ACTION_ENTITY_ID_1, OBJECT_ADMIN_ACTION_ENTITY_ID_3), new ExpectedResult(
                    OBJECT_ADMIN_ACTION_ENTITY_ID_2));
        }

        @Test
        void getMediasMarkedForDeletion_shouldGroupByCourtRoom() {
            setupStandingData();
            when(objectAdminActionEntity2.getMedia().getCourtroom().getId()).thenReturn(123);
            triggerAndValidate(List.of(objectAdminActionEntity1, objectAdminActionEntity2, objectAdminActionEntity3),
                               new ExpectedResult(OBJECT_ADMIN_ACTION_ENTITY_ID_1, OBJECT_ADMIN_ACTION_ENTITY_ID_3), new ExpectedResult(
                    OBJECT_ADMIN_ACTION_ENTITY_ID_2));
        }

        @Test
        void getMediasMarkedForDeletion_shouldGroupByStart() {
            setupStandingData();
            when(objectAdminActionEntity2.getMedia().getStart()).thenReturn(OffsetDateTime.now().plusMinutes(2));
            triggerAndValidate(List.of(objectAdminActionEntity1, objectAdminActionEntity2, objectAdminActionEntity3),
                               new ExpectedResult(OBJECT_ADMIN_ACTION_ENTITY_ID_1, OBJECT_ADMIN_ACTION_ENTITY_ID_3), new ExpectedResult(
                    OBJECT_ADMIN_ACTION_ENTITY_ID_2));
        }

        @Test
        void getMediasMarkedForDeletion_shouldGroupByEnd() {
            setupStandingData();
            when(objectAdminActionEntity2.getMedia().getEnd()).thenReturn(OffsetDateTime.now().plusMinutes(2));
            triggerAndValidate(List.of(objectAdminActionEntity1, objectAdminActionEntity2, objectAdminActionEntity3),
                               new ExpectedResult(OBJECT_ADMIN_ACTION_ENTITY_ID_1, OBJECT_ADMIN_ACTION_ENTITY_ID_3), new ExpectedResult(
                    OBJECT_ADMIN_ACTION_ENTITY_ID_2));
        }
    }


    private ObjectAdminActionEntity createObjectAdminActionEntity(String ticketRef,
                                                                  int userAccountId,
                                                                  int objectHiddenReasonEntityId,
                                                                  int courtroomEntityId,
                                                                  OffsetDateTime start, OffsetDateTime end,
                                                                  int objectAdminActionEntityId) {

        UserAccountEntity userAccount = createUserAccountWithId(userAccountId);
        ObjectHiddenReasonEntity objectHiddenReasonEntity = createObjectHiddenReasonEntity(objectHiddenReasonEntityId);
        CourtroomEntity courtroomEntity = createCourtroomEntity(courtroomEntityId);


        ObjectAdminActionEntity objectAdminActionEntity = mock(ObjectAdminActionEntity.class);
        when(objectAdminActionEntity.getTicketReference()).thenReturn(ticketRef);
        when(objectAdminActionEntity.getHiddenBy()).thenReturn(userAccount);
        when(objectAdminActionEntity.getObjectHiddenReason()).thenReturn(objectHiddenReasonEntity);
        when(objectAdminActionEntity.getId()).thenReturn(objectAdminActionEntityId);

        MediaEntity media = mock(MediaEntity.class);
        when(media.getCourtroom()).thenReturn(courtroomEntity);
        when(media.getStart()).thenReturn(start);
        when(media.getEnd()).thenReturn(end);
        when(objectAdminActionEntity.getMedia()).thenReturn(media);
        return objectAdminActionEntity;
    }

    private CourtroomEntity createCourtroomEntity(int id) {
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        when(courtroomEntity.getId()).thenReturn(id);
        return courtroomEntity;
    }

    private ObjectHiddenReasonEntity createObjectHiddenReasonEntity(int id) {
        ObjectHiddenReasonEntity objectHiddenReasonEntity = mock(ObjectHiddenReasonEntity.class);
        when(objectHiddenReasonEntity.getId()).thenReturn(id);
        return objectHiddenReasonEntity;
    }

    private UserAccountEntity createUserAccountWithId(int id) {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userAccount.getId()).thenReturn(id);
        return userAccount;
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
        assertEquals(mediaEntity.getId(), response.getFirst().getId());
        String responseString = objectMapper.writeValueAsString(response);
        String expectedString = "[" + MEDIA_ID_5 + "]";
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaEntityById_shouldReutrnMediaEntity_ifOneExists() {
        MediaEntity mediaEntity = mock(MediaEntity.class);
        when(mediaRepository.findById(1)).thenReturn(Optional.of(mediaEntity));

        assertThat(mediaRequestService.getMediaEntityById(1))
            .isEqualTo(mediaEntity);
        verify(mediaRepository).findById(1);
    }

    @Test
    void getMediaEntityById_shouldThrowException_ifNoMediaEntityExists() {
        when(mediaRepository.findById(1)).thenReturn(Optional.empty());

        DartsApiException exception = assertThrows(DartsApiException.class, () -> mediaRequestService.getMediaEntityById(1));

        assertThat(exception.getError()).isEqualTo(AudioApiError.MEDIA_NOT_FOUND);
    }


    @Nested
    @DisplayName("AdminVersionedMediaResponse getMediaVersionsById(Integer id)")
    class GetMediaVersionsById {
        @Test
        void getMediaVersionsById_shouldThrowException_whenChronicleIdIsNull() {
            MediaEntity mediaEntity = mock(MediaEntity.class);
            mediaEntity.setChronicleId(null);
            doReturn(mediaEntity).when(mediaRequestService).getMediaEntityById(123);

            DartsApiException exception = assertThrows(DartsApiException.class, () -> mediaRequestService.getMediaVersionsById(123));
            assertThat(exception.getError()).isEqualTo(CommonApiError.INTERNAL_SERVER_ERROR);
            assertThat(exception.getMessage())
                .isEqualTo("Internal server error. Media 123 has a Chronicle Id that is null. As such we can not ensure accurate results are returned");
        }

        @Test
        void getMediaVersionsById_shouldReturnEmptyVersionList_whenNoMediaVersionsExist() {
            final String chronicleId = "someChronicleId";
            MediaEntity mediaEntity = createMediaEntity(true, chronicleId, OffsetDateTime.now());
            doReturn(mediaEntity).when(mediaRequestService).getMediaEntityById(123);
            when(mediaRepository.findAllByChronicleId(chronicleId)).thenReturn(List.of(mediaEntity));

            AdminVersionedMediaResponse response = mock(AdminVersionedMediaResponse.class);
            when(getAdminMediaResponseMapper.mapAdminVersionedMediaResponse(mediaEntity, List.of())).thenReturn(response);

            assertThat(mediaRequestService.getMediaVersionsById(123))
                .isEqualTo(response);

            verify(mediaRepository).findAllByChronicleId(chronicleId);
            verify(getAdminMediaResponseMapper).mapAdminVersionedMediaResponse(mediaEntity, List.of());
            verify(mediaRequestService).getMediaEntityById(123);
        }

        @Test
        void getMediaVersionsById_shouldReturnVersionsAndCurrentMedia_whenVersionsExist() {
            final String chronicleId = "someChronicleId";
            OffsetDateTime now = OffsetDateTime.now();
            MediaEntity currentMediaEntity = createMediaEntity(true, chronicleId, now);
            MediaEntity versionedMediaEntity1 = createMediaEntity(null, chronicleId, now.plusMinutes(2));
            MediaEntity versionedMediaEntity2 = createMediaEntity(false, chronicleId, now.plusMinutes(1));

            doReturn(currentMediaEntity).when(mediaRequestService).getMediaEntityById(123);
            when(mediaRepository.findAllByChronicleId(chronicleId))
                .thenReturn(List.of(currentMediaEntity, versionedMediaEntity2, versionedMediaEntity1));

            AdminVersionedMediaResponse response = mock(AdminVersionedMediaResponse.class);

            List<MediaEntity> expectedVersioendMedia = List.of(versionedMediaEntity1, versionedMediaEntity2);
            when(getAdminMediaResponseMapper.mapAdminVersionedMediaResponse(currentMediaEntity, expectedVersioendMedia))
                .thenReturn(response);

            assertThat(mediaRequestService.getMediaVersionsById(123))
                .isEqualTo(response);

            verify(mediaRepository).findAllByChronicleId(chronicleId);
            verify(getAdminMediaResponseMapper).mapAdminVersionedMediaResponse(currentMediaEntity, expectedVersioendMedia);
            verify(mediaRequestService).getMediaEntityById(123);
        }


        @Test
        void getMediaVersionsById_shouldReturnNullCurrentVersion_ifAllMediaIsCurrentFlase() {
            final String chronicleId = "someChronicleId";
            OffsetDateTime now = OffsetDateTime.now();
            MediaEntity versionedMediaEntity1 = createMediaEntity(null, chronicleId, now.plusMinutes(2));
            MediaEntity versionedMediaEntity2 = createMediaEntity(false, chronicleId, now.plusMinutes(1));

            doReturn(versionedMediaEntity1).when(mediaRequestService).getMediaEntityById(123);


            when(mediaRepository.findAllByChronicleId(chronicleId))
                .thenReturn(List.of(versionedMediaEntity2, versionedMediaEntity1));

            AdminVersionedMediaResponse response = mock(AdminVersionedMediaResponse.class);

            List<MediaEntity> expectedVersioendMedia = List.of(versionedMediaEntity1, versionedMediaEntity2);
            when(getAdminMediaResponseMapper.mapAdminVersionedMediaResponse(null, expectedVersioendMedia))
                .thenReturn(response);

            assertThat(mediaRequestService.getMediaVersionsById(123))
                .isEqualTo(response);

            verify(mediaRepository).findAllByChronicleId(chronicleId);
            verify(getAdminMediaResponseMapper).mapAdminVersionedMediaResponse(null, expectedVersioendMedia);
            verify(mediaRequestService).getMediaEntityById(123);

        }

        @Test
        void getMediaVersionsById_shouldReturnLastCreatedMedia_ifMultipleIsCurrentTrueExist() {
            final String chronicleId = "someChronicleId";
            OffsetDateTime now = OffsetDateTime.now();
            MediaEntity currentMediaEntity1 = createMediaEntity(true, chronicleId, now.plusMinutes(2));
            MediaEntity currentMediaEntity2 = createMediaEntity(true, chronicleId, now);
            MediaEntity currentMediaEntity3 = createMediaEntity(true, chronicleId, now.plusMinutes(1));

            MediaEntity versionedMediaEntity1 = createMediaEntity(null, chronicleId, now.plusMinutes(2));
            MediaEntity versionedMediaEntity2 = createMediaEntity(false, chronicleId, now.plusMinutes(1));

            doReturn(currentMediaEntity1).when(mediaRequestService).getMediaEntityById(123);
            when(mediaRepository.findAllByChronicleId(chronicleId))
                .thenReturn(List.of(currentMediaEntity1, currentMediaEntity2, currentMediaEntity3, versionedMediaEntity2, versionedMediaEntity1));
            AdminVersionedMediaResponse response = mock(AdminVersionedMediaResponse.class);


            List<MediaEntity> expectedVersioendMedia = List.of(currentMediaEntity3, currentMediaEntity2, versionedMediaEntity1, versionedMediaEntity2);
            when(getAdminMediaResponseMapper.mapAdminVersionedMediaResponse(currentMediaEntity1, expectedVersioendMedia))
                .thenReturn(response);

            assertThat(mediaRequestService.getMediaVersionsById(123))
                .isEqualTo(response);

            verify(mediaRepository).findAllByChronicleId(chronicleId);
            verify(getAdminMediaResponseMapper).mapAdminVersionedMediaResponse(currentMediaEntity1, expectedVersioendMedia);
            verify(mediaRequestService).getMediaEntityById(123);

        }

        private MediaEntity createMediaEntity(Boolean isCurrent, String chronicleId, OffsetDateTime offsetDateTime) {
            return PersistableFactory.getMediaTestData().someMinimalBuilder()
                .chronicleId(chronicleId)
                .isCurrent(isCurrent)
                .createdDateTime(offsetDateTime)
                .build()
                .getEntity();
        }
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

    @Nested
    class ToGetAdminMediasMarkedForDeletionItemTests {

        private final CourthouseMapper courthouseMapper = new CourthouseMapperImpl();
        private final CourtroomMapper courtroomMapper = new CourtroomMapperImpl();
        private final ObjectActionMapper objectActionMapper = new ObjectActionMapperImpl();
        private final AdminMarkedForDeletionMapper adminMarkedForDeletionMapper = new AdminMarkedForDeletionMapperImpl();

        @BeforeEach
        void beforeEach() {
            ReflectionTestUtils.setField(mediaRequestService, "courthouseMapper", courthouseMapper);
            ReflectionTestUtils.setField(mediaRequestService, "courtroomMapper", courtroomMapper);
            ReflectionTestUtils.setField(mediaRequestService, "objectActionMapper", objectActionMapper);
            ReflectionTestUtils.setField(mediaRequestService, "adminMarkedForDeletionMapper", adminMarkedForDeletionMapper);
        }

        @Test
        void toGetAdminMediasMarkedForDeletionItem_shouldMapAllFieldsCorrectly() {
            //Data setup
            CourthouseEntity courthouseEntity = mock(CourthouseEntity.class);
            when(courthouseEntity.getId()).thenReturn(1);
            when(courthouseEntity.getDisplayName()).thenReturn("courthouseName");


            CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
            when(courtroomEntity.getId()).thenReturn(2);
            when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
            when(courtroomEntity.getName()).thenReturn("courtRoomName");


            final OffsetDateTime start = OffsetDateTime.now();
            final OffsetDateTime end = OffsetDateTime.now().plusDays(1);


            MediaEntity baseMediaEntity = mock(MediaEntity.class);
            when(baseMediaEntity.getStart()).thenReturn(start);
            when(baseMediaEntity.getEnd()).thenReturn(end);
            when(baseMediaEntity.getCourtroom()).thenReturn(courtroomEntity);


            when(baseMediaEntity.getId()).thenReturn(321);
            when(baseMediaEntity.getChannel()).thenReturn(1);
            when(baseMediaEntity.getTotalChannels()).thenReturn(4);
            when(baseMediaEntity.getIsCurrent()).thenReturn(true);
            when(baseMediaEntity.getChronicleId()).thenReturn("chronicleId1");

            when(mediaRepository.getVersionCount("chronicleId1")).thenReturn(2);


            UserAccountEntity userAccount = mock(UserAccountEntity.class);
            when(userAccount.getId()).thenReturn(3);

            ObjectHiddenReasonEntity objectHiddenReasonEntity = mock(ObjectHiddenReasonEntity.class);
            when(objectHiddenReasonEntity.getId()).thenReturn(4);


            ObjectAdminActionEntity baseObjectAdminActionEntity = mock(ObjectAdminActionEntity.class);
            when(baseObjectAdminActionEntity.getMedia()).thenReturn(baseMediaEntity);
            when(baseObjectAdminActionEntity.getComments()).thenReturn("Comment1");
            when(baseObjectAdminActionEntity.getTicketReference()).thenReturn("ticketReference1");
            when(baseObjectAdminActionEntity.getObjectHiddenReason()).thenReturn(objectHiddenReasonEntity);
            when(baseObjectAdminActionEntity.getHiddenBy()).thenReturn(userAccount);

            MediaEntity media2 = mock(MediaEntity.class);
            when(media2.getId()).thenReturn(4321);
            when(media2.getChannel()).thenReturn(2);
            when(media2.getTotalChannels()).thenReturn(4);
            when(media2.getIsCurrent()).thenReturn(true);
            when(media2.getChronicleId()).thenReturn("chronicleId2");

            ObjectAdminActionEntity objectAdminActionEntity2 = mock(ObjectAdminActionEntity.class);
            when(objectAdminActionEntity2.getMedia()).thenReturn(media2);
            when(objectAdminActionEntity2.getComments()).thenReturn("Comment2");

            when(mediaRepository.getVersionCount("chronicleId1")).thenReturn(2);
            when(mediaRepository.getVersionCount("chronicleId2")).thenReturn(3);

            //Run
            GetAdminMediasMarkedForDeletionItem result = mediaRequestService.toGetAdminMediasMarkedForDeletionItem(
                List.of(baseObjectAdminActionEntity, objectAdminActionEntity2));

            //Check media
            assertThat(result.getMedia()).hasSize(2);
            assertMedia(result.getMedia().getFirst(), 321, 1, 4, true, 2);
            assertMedia(result.getMedia().get(1), 4321, 2, 4, true, 3);
            //Check courthouse
            assertCourthouse(result.getCourthouse(), "courthouseName", 1);
            assertCourtroom(result.getCourtroom(), "courtRoomName", 2);
            assertAdminAction(result.getAdminAction(), "ticketReference1", 3, 4,
                              List.of("Comment1", "Comment2"));
            assertThat(result.getStartAt()).isEqualTo(start);
            assertThat(result.getEndAt()).isEqualTo(end);
            verify(mediaRepository).getVersionCount("chronicleId1");
            verify(mediaRepository).getVersionCount("chronicleId2");
        }

        private void assertMedia(GetAdminMediasMarkedForDeletionMediaItem actual,
                                 int expectedId,
                                 int expectedChannel,
                                 int expectedTotalChannels,
                                 boolean expectedIsCurrent,
                                 int expectedVersionCount) {
            assertThat(actual.getId()).isEqualTo(expectedId);
            assertThat(actual.getChannel()).isEqualTo(expectedChannel);
            assertThat(actual.getTotalChannels()).isEqualTo(expectedTotalChannels);
            assertThat(actual.getIsCurrent()).isEqualTo(expectedIsCurrent);
            assertThat(actual.getVersionCount()).isEqualTo(expectedVersionCount);
        }

        private void assertAdminAction(GetAdminMediasMarkedForDeletionAdminAction actual,
                                       String expectedTicketReference,
                                       int expectedHiddenById,
                                       int expectedReasonId,
                                       List<String> expectedComments) {
            assertThat(actual.getTicketReference()).isEqualTo(expectedTicketReference);
            assertThat(actual.getHiddenById()).isEqualTo(expectedHiddenById);
            assertThat(actual.getReasonId()).isEqualTo(expectedReasonId);
            assertThat(actual.getComments()).isEqualTo(expectedComments);
        }

        private void assertCourtroom(AdminMediaCourtroomResponse actual, String expectedCourtRoomName, int expectedId) {
            assertThat(actual.getId()).isEqualTo(expectedId);
            assertThat(actual.getName()).isEqualTo(expectedCourtRoomName);
        }

        private void assertCourthouse(AdminMediaCourthouseResponse actual, String expectedCourtHouseName, int expectedId) {
            assertThat(actual.getId()).isEqualTo(expectedId);
            assertThat(actual.getDisplayName()).isEqualTo(expectedCourtHouseName);
        }
    }

    @Nested
    class PatchMediasByIdTests {

        @ParameterizedTest
        @ValueSource(booleans = false)
        @NullSource
        void shouldThrowException_whenIsCurrentIsTrue(Boolean isCurrent) {
            PatchAdminMediasByIdRequest request = new PatchAdminMediasByIdRequest(isCurrent);
            DartsApiException exception = assertThrows(DartsApiException.class, () -> mediaRequestService.patchMediasById(1, request));
            assertThat(exception.getError()).isEqualTo(CommonApiError.INVALID_REQUEST);
        }

        @Test
        void shouldThrowException_whenMediaIsAlreadyIsCurrent() {
            PatchAdminMediasByIdRequest request = new PatchAdminMediasByIdRequest(true);
            MediaEntity media = mock(MediaEntity.class);
            doReturn(media).when(mediaRequestService).getMediaEntityById(123);
            when(media.getIsCurrent()).thenReturn(true);
            DartsApiException exception = assertThrows(DartsApiException.class, () -> mediaRequestService.patchMediasById(123, request));
            assertThat(exception.getError()).isEqualTo(AudioApiError.MEDIA_ALREADY_CURRENT);
        }

        @Test
        void shouldUpdateMediaIsCurrent_whenMediaIsNotCurrent() {
            PatchAdminMediasByIdRequest request = new PatchAdminMediasByIdRequest(true);
            MediaEntity media = mock(MediaEntity.class);
            doReturn(media).when(mediaRequestService).getMediaEntityById(123);
            when(media.getIsCurrent()).thenReturn(false);
            when(media.getChronicleId()).thenReturn("chronicleId123");
            when(media.getId()).thenReturn(123);

            mediaRequestService.patchMediasById(123, request);

            verify(media).setIsCurrent(true);
            verify(mediaRepository).save(media);
            verify(mediaRepository).setAllAssociatedMediaToIsCurrentFalseExcludingMediaId("chronicleId123", 123);
        }
    }

    @Nested
    class AdminHideOrShowMediaByIdTests {

        private MediaEntity mediaEntity;

        private MockedStatic<GetAdminMediaResponseMapper> mediaResponseMapperMockedStatic;

        @BeforeEach
        void setUp() {
            mediaResponseMapperMockedStatic = Mockito.mockStatic(GetAdminMediaResponseMapper.class);

            mediaEntity = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1)
                .build()
                .getEntity();
            when(mediaRepository.findByIdIncludeDeleted(1))
                .thenReturn(Optional.of(mediaEntity));
        }

        @AfterEach
        void tearDown() {
            if (mediaResponseMapperMockedStatic != null) {
                mediaResponseMapperMockedStatic.close();
            }
        }

        @Test
        void shouldInvokeHideFunctionality_whenMediaHideRequestHasIsHiddenTrue() {
            // Given
            ObjectHiddenReasonEntity objectHiddenReasonEntity = new ObjectHiddenReasonEntity();
            when(hiddenReasonRepository.findById(0))
                .thenReturn(Optional.of(objectHiddenReasonEntity));

            ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
            when(objectAdminActionRepository.findByMedia_Id(1))
                .thenReturn(Collections.singletonList(objectAdminActionEntity));

            MediaHideRequest mediaHideRequest = new MediaHideRequest();
            mediaHideRequest.setIsHidden(true);

            AdminActionRequest adminActionRequest = new AdminActionRequest();
            adminActionRequest.setReasonId(0);
            adminActionRequest.setTicketReference("Some reference");
            adminActionRequest.setComments("Some comments");

            mediaHideRequest.setAdminAction(adminActionRequest);

            // When
            mediaRequestService.adminHideOrShowMediaById(1, mediaHideRequest);

            // Then
            var expectedActionProperties = new ApplyAdminActionComponent.AdminActionProperties("Some reference",
                                                                                               "Some comments",
                                                                                               objectHiddenReasonEntity);
            verify(applyAdminActionComponent).applyAdminActionToAllVersions(eq(mediaEntity), eq(expectedActionProperties));
        }

        @Test
        void shouldThrowException_whenProvidedHiddenReasonDoesNotExist() {
            // Given
            when(hiddenReasonRepository.findById(0))
                .thenReturn(Optional.empty());

            MediaHideRequest mediaHideRequest = new MediaHideRequest();
            mediaHideRequest.setIsHidden(true);

            AdminActionRequest adminActionRequest = new AdminActionRequest();
            adminActionRequest.setReasonId(0);
            adminActionRequest.setTicketReference("Some reference");
            adminActionRequest.setComments("Some comments");

            mediaHideRequest.setAdminAction(adminActionRequest);

            // When
            DartsApiException exception = assertThrows(DartsApiException.class, () ->
                mediaRequestService.adminHideOrShowMediaById(1, mediaHideRequest));

            // Then
            assertEquals("Hide reason is incorrect", exception.getMessage());
            verifyNoInteractions(objectAdminActionRepository);
            verifyNoInteractions(applyAdminActionComponent);
        }

        @Test
        void shouldInvokeUnhideFunctionality_whenMediaHideRequestHasIsHiddenFalse() {
            // Given
            MediaHideRequest mediaHideRequest = new MediaHideRequest();
            mediaHideRequest.setIsHidden(false);

            // When
            mediaRequestService.adminHideOrShowMediaById(1, mediaHideRequest);

            // Then
            verify(removeAdminActionComponent).removeAdminActionFromAllVersions(eq(mediaEntity));
        }

        @Test
        void shouldThrowException_whenNoMediaIsFound() {
            // Given
            when(mediaRepository.findByIdIncludeDeleted(1))
                .thenReturn(Optional.empty());

            MediaHideRequest mediaHideRequest = new MediaHideRequest();

            // When
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                           () -> mediaRequestService.adminHideOrShowMediaById(1, mediaHideRequest));

            // Then
            assertEquals(exception.getMessage(), "Media not found, expected this to be pre-validated");
        }

    }

}