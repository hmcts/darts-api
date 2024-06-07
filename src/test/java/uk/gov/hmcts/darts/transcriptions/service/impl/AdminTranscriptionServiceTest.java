package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.AdminActionRequest;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;
import uk.gov.hmcts.darts.transcriptions.service.AdminTranscriptionService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionSearchQuery;
import uk.gov.hmcts.darts.transcriptions.validator.TranscriptionDocumentHideOrShowValidator;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTranscriptionServiceTest {

    private AdminTranscriptionService adminTranscriptionSearchService;

    @Mock
    private TranscriptionSearchQuery transcriptionSearchQuery;

    @Mock
    private TranscriptionRepository transcriptionRepository;

    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    @Mock
    private TranscriptionResponseMapper transcriptionResponseMapper;

    @Mock
    private UserAccountExistsValidator userAccountExistsValidator;

    @Mock
    private TranscriptionDocumentHideOrShowValidator transcriptionDocumentHideOrShowValidator;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private UserIdentity userIdentity;

    @Captor
    ArgumentCaptor<ObjectAdminActionEntity> objectAdminActionEntityArgumentCaptor;

    @Captor
    ArgumentCaptor<TranscriptionDocumentEntity> transcriptionDocumentEntityArgumentCaptor;

    @BeforeEach
    void setUp() {
        adminTranscriptionSearchService
            = new AdminTranscriptionServiceImpl(transcriptionSearchQuery,
                                                transcriptionRepository,
                                                transcriptionDocumentRepository,
                                                transcriptionResponseMapper,
                                                userAccountExistsValidator,
                                                transcriptionDocumentHideOrShowValidator,
                                                objectAdminActionRepository,
                                                objectHiddenReasonRepository,
                                                userIdentity);
  }

    @Test
    void returnsEmptyIfOwnerFilterProvidedWithNoMatches() {
        when(transcriptionSearchQuery.findTranscriptionsIdsCurrentlyOwnedBy("some-owner")).thenReturn(List.of());

        var results = adminTranscriptionSearchService.searchTranscriptions(
            new TranscriptionSearchRequest().owner("some-owner"));

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void returnsEmptyIfOwnedByFilterResultsDontIntersectWithProvidedTranscriptionIdFilter() {
        when(transcriptionSearchQuery.findTranscriptionsIdsCurrentlyOwnedBy("some-owner")).thenReturn(List.of(2, 3, 4));

        var results = adminTranscriptionSearchService.searchTranscriptions(
            new TranscriptionSearchRequest()
                .owner("some-owner")
                .transcriptionId(1));

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void mapsTranscriptionsSearchResultsToTranscriptionSearchResponse() {
        var transcriptionSearchResults = someSetOfTranscriptionSearchResult(3);
        when(transcriptionSearchQuery.searchTranscriptions(any(TranscriptionSearchRequest.class), any()))
            .thenReturn(transcriptionSearchResults);

        var searchResponses = adminTranscriptionSearchService.searchTranscriptions(new TranscriptionSearchRequest());

        assertThat(searchResponses).extracting("transcriptionId").containsExactly(1, 2, 3);
        assertThat(searchResponses).extracting("caseNumber").containsExactly("case-number-1", "case-number-2", "case-number-3");
        assertThat(searchResponses).extracting("courthouseId").containsExactly(11, 12, 13);
        assertThat(searchResponses).extracting("hearingDate").containsExactly(
                LocalDate.parse("2020-01-02"),
                LocalDate.parse("2020-01-03"),
                LocalDate.parse("2020-01-04"));
        assertThat(searchResponses).extracting("requestedAt").containsExactly(
                OffsetDateTime.parse("2021-02-03T00:00:00Z"),
                OffsetDateTime.parse("2021-02-04T00:00:00Z"),
                OffsetDateTime.parse("2021-02-05T00:00:00Z"));
        assertThat(searchResponses).extracting("transcriptionStatusId").containsExactly(21, 22, 23);
        assertThat(searchResponses).extracting("isManualTranscription").containsExactly(false, true, false);

        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void testGetTranscriptionDetailsForUser() {
        Integer userId = 200;

        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        TranscriptionEntity transcriptionEntity1 = new TranscriptionEntity();

        List<TranscriptionEntity> transcriptionEntityList = new ArrayList<>();
        transcriptionEntityList.add(transcriptionEntity);
        transcriptionEntityList.add(transcriptionEntity1);

        GetTranscriptionDetailAdminResponse response = new GetTranscriptionDetailAdminResponse();
        GetTranscriptionDetailAdminResponse response1 = new GetTranscriptionDetailAdminResponse();
        OffsetDateTime dateTimeOfSearch = OffsetDateTime.now();

        when(transcriptionRepository.findTranscriptionForUserOnOrAfterDate(userId, dateTimeOfSearch))
            .thenReturn(transcriptionEntityList);
        when(transcriptionResponseMapper.mapTransactionEntityToTransactionDetails(Mockito.eq(transcriptionEntity))).thenReturn(response);
        when(transcriptionResponseMapper.mapTransactionEntityToTransactionDetails(Mockito.eq(transcriptionEntity1))).thenReturn(response1);

        List<GetTranscriptionDetailAdminResponse> fndTranscriptions = adminTranscriptionSearchService
            .getTranscriptionsForUser(userId, dateTimeOfSearch);

        Assertions.assertEquals(transcriptionEntityList.size(), fndTranscriptions.size());
        Assertions.assertTrue(fndTranscriptions.contains(response));
        Assertions.assertTrue(fndTranscriptions.contains(response1));
    }

    @Test
    void testGetTranscriptionDetailsNoTranscriptionsFound() {
        Integer userId = 200;
        OffsetDateTime dateTimeOfSearch = OffsetDateTime.now();

        when(transcriptionRepository.findTranscriptionForUserOnOrAfterDate(userId, dateTimeOfSearch))
            .thenReturn(new ArrayList<>());

        List<GetTranscriptionDetailAdminResponse> fndTranscriptions = adminTranscriptionSearchService
                .getTranscriptionsForUser(userId, dateTimeOfSearch);

        Assertions.assertTrue(fndTranscriptions.isEmpty());
    }

    @Test
    void testGetTranscriptionDetailsUserNotExist() {
        Integer userId = 200;
        OffsetDateTime dateTimeOfSearch = OffsetDateTime.now();

        Mockito.doThrow(new DartsApiException(UserManagementError.USER_NOT_FOUND)).when(userAccountExistsValidator).validate(userId);

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class, () -> {
            adminTranscriptionSearchService
                .getTranscriptionsForUser(userId, dateTimeOfSearch);
        });
        Assertions.assertEquals(UserManagementError.USER_NOT_FOUND, exception.getError());
    }

    @Test
    void getTranscriptionDocumentByIdNotExist() {
        Integer transDocId = 200;

        when(transcriptionDocumentRepository.findById(transDocId)).thenReturn(Optional.empty());

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class,
                        () -> adminTranscriptionSearchService.getTranscriptionDocumentById(transDocId));

        Assertions.assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND, exception.getError());
    }

    @Test
    void getTranscriptionDocumentById() {
        Integer transDocId = 200;

        TranscriptionDocumentEntity transcriptionDocumentEntity = mock(TranscriptionDocumentEntity.class);
        GetTranscriptionDocumentByIdResponse expectedResponse = new GetTranscriptionDocumentByIdResponse();
        when(transcriptionDocumentRepository.findById(Mockito.eq(transDocId))).thenReturn(Optional.ofNullable(transcriptionDocumentEntity));
        when(transcriptionResponseMapper.getSearchByTranscriptionDocumentId(Mockito.eq(transcriptionDocumentEntity))).thenReturn(expectedResponse);

        GetTranscriptionDocumentByIdResponse actualResponse = adminTranscriptionSearchService.getTranscriptionDocumentById(transDocId);

        Assertions.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void testTranscriptionDocumentHide() {
        TranscriptionDocumentHideRequest request = new TranscriptionDocumentHideRequest();
        request.setIsHidden(true);
        setupTestTranscriptionDocumentHide(request);
    }

    @Test
    void testTranscriptionDocumentHideDefaultIsHidden() {
        TranscriptionDocumentHideRequest request = new TranscriptionDocumentHideRequest();
        setupTestTranscriptionDocumentHide(request);
    }

    void setupTestTranscriptionDocumentHide(TranscriptionDocumentHideRequest request) {
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

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        when(transcriptionDocumentRepository.findById(hideOrShowTranscriptionDocument)).thenReturn(Optional.of(transcriptionDocumentEntity));
        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        when(transcriptionDocumentRepository.saveAndFlush(transcriptionDocumentEntityArgumentCaptor.capture())).thenReturn(transcriptionDocumentEntity);
        Integer objectAdminActionId = -1;
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setId(objectAdminActionId);
        ObjectHiddenReasonEntity objectHiddenReasonEntity = new ObjectHiddenReasonEntity();

        TranscriptionDocumentHideResponse expectedResponse = new TranscriptionDocumentHideResponse();

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(objectHiddenReasonEntity));

        when(objectAdminActionRepository.saveAndFlush(objectAdminActionEntityArgumentCaptor.capture())).thenReturn(objectAdminActionEntity);

        when(transcriptionResponseMapper.mapHideOrShowResponse(transcriptionDocumentEntity, objectAdminActionEntity)).thenReturn(expectedResponse);
        when(objectAdminActionRepository.findById(objectAdminActionId)).thenReturn(Optional.of(objectAdminActionEntity));

        //run the test
        TranscriptionDocumentHideResponse actualResponse
            = adminTranscriptionSearchService.hideOrShowTranscriptionDocumentById(hideOrShowTranscriptionDocument, request);


        // make the assertion
        Assertions.assertTrue(transcriptionDocumentEntityArgumentCaptor.getValue().isHidden());
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
    void testTranscriptionDocumentShow() {
        TranscriptionDocumentHideRequest request = new TranscriptionDocumentHideRequest();
        request.setIsHidden(false);

        Integer hideOrShowTranscriptionDocument = 343;
        Integer reasonId = 555;

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(reasonId);
        request.setAdminAction(adminActionRequest);

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        when(transcriptionDocumentRepository.findById(hideOrShowTranscriptionDocument)).thenReturn(Optional.of(transcriptionDocumentEntity));

        Integer objectAdminActionEntityId = 1000;
        Integer objectAdminActionEntityId1 = 1001;

        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setId(objectAdminActionEntityId);
        ObjectAdminActionEntity objectAdminActionEntity1 = new ObjectAdminActionEntity();
        objectAdminActionEntity1.setId(objectAdminActionEntityId1);

        when(transcriptionDocumentRepository.saveAndFlush(transcriptionDocumentEntityArgumentCaptor.capture())).thenReturn(transcriptionDocumentEntity);
        when(objectAdminActionRepository
                 .findByTranscriptionDocument_Id(hideOrShowTranscriptionDocument)).thenReturn(List.of(objectAdminActionEntity, objectAdminActionEntity1));

        TranscriptionDocumentHideResponse expectedResponse = new TranscriptionDocumentHideResponse();

        when(transcriptionResponseMapper.mapHideOrShowResponse(transcriptionDocumentEntity, null)).thenReturn(expectedResponse);

        // run the test
        TranscriptionDocumentHideResponse actualResponse
            = adminTranscriptionSearchService.hideOrShowTranscriptionDocumentById(hideOrShowTranscriptionDocument, request);

        // make the assertion
        Assertions.assertFalse(transcriptionDocumentEntityArgumentCaptor.getValue().isHidden());
        Assertions.assertEquals(expectedResponse, actualResponse);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId1);
    }

    private static Set<TranscriptionSearchResult> someSetOfTranscriptionSearchResult(int quantity) {
        return rangeClosed(1, quantity).mapToObj(i -> createTranscription(i)).collect(toSet());
    }

    private static TranscriptionSearchResult createTranscription(int seed) {
        return new TranscriptionSearchResult(
            seed,
            "case-number-" + seed,
            seed + 10,
            LocalDate.parse("2020-01-01").plusDays(seed),
            OffsetDateTime.parse("2021-02-02T00:00:00Z").plusDays(seed),
            seed + 20,
            seed % 2 == 0);
    }

}