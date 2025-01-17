package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.AdminActionRequest;
import uk.gov.hmcts.darts.transcriptions.model.AdminApproveDeletionResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionSearchQuery;
import uk.gov.hmcts.darts.transcriptions.validator.TranscriptionApproveMarkForDeletionValidator;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.HIDE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UNHIDE_TRANSCRIPTION;

@ExtendWith(MockitoExtension.class)
class AdminTranscriptionServiceTest {

    private AdminTranscriptionServiceImpl adminTranscriptionService;

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
    private TranscriptionApproveMarkForDeletionValidator transcriptionApproveMarkForDeletionValidator;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private AuditApi auditApi;

    @Captor
    ArgumentCaptor<ObjectAdminActionEntity> objectAdminActionEntityArgumentCaptor;

    @Captor
    ArgumentCaptor<TranscriptionDocumentEntity> transcriptionDocumentEntityArgumentCaptor;

    @BeforeEach
    void setUp() {
        adminTranscriptionService
            = new AdminTranscriptionServiceImpl(transcriptionSearchQuery,
                                                transcriptionRepository,
                                                transcriptionDocumentRepository,
                                                transcriptionResponseMapper,
                                                userAccountExistsValidator,
                                                transcriptionDocumentHideOrShowValidator,
                                                objectAdminActionRepository,
                                                objectHiddenReasonRepository,
                                                userIdentity,
                                                transcriptionApproveMarkForDeletionValidator,
                                                auditApi);
    }

    private void updateManualDeletion(boolean manualDeletionEnabled) {
        this.adminTranscriptionService = spy(adminTranscriptionService);
        when(adminTranscriptionService.isManualDeletionEnabled()).thenReturn(manualDeletionEnabled);
    }

    @Test
    void returnsEmptyIfOwnerFilterProvidedWithNoMatches() {
        when(transcriptionSearchQuery.findTranscriptionsIdsCurrentlyOwnedBy("some-owner")).thenReturn(List.of());

        var results = adminTranscriptionService.searchTranscriptions(
            new TranscriptionSearchRequest().owner("some-owner"));

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void returnsEmptyIfOwnedByFilterResultsDontIntersectWithProvidedTranscriptionIdFilter() {
        when(transcriptionSearchQuery.findTranscriptionsIdsCurrentlyOwnedBy("some-owner")).thenReturn(List.of(2, 3, 4));

        var results = adminTranscriptionService.searchTranscriptions(
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

        var searchResponses = adminTranscriptionService.searchTranscriptions(new TranscriptionSearchRequest());

        assertThat(searchResponses).extracting("transcriptionId").containsExactly(3, 2, 1);
        assertThat(searchResponses).extracting("caseNumber").containsExactly("case-number-3", "case-number-2", "case-number-1");
        assertThat(searchResponses).extracting("courthouseId").containsExactly(13, 12, 11);
        assertThat(searchResponses).extracting("hearingDate").containsExactly(
            LocalDate.parse("2020-01-04"),
            LocalDate.parse("2020-01-03"),
            LocalDate.parse("2020-01-02"));
        assertThat(searchResponses).extracting("requestedAt").containsExactly(
            OffsetDateTime.parse("2021-02-05T00:00:00Z"),
            OffsetDateTime.parse("2021-02-04T00:00:00Z"),
            OffsetDateTime.parse("2021-02-03T00:00:00Z"));
        assertThat(searchResponses).extracting("transcriptionStatusId").containsExactly(23, 22, 21);
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
        when(transcriptionResponseMapper.mapTransactionEntityToTransactionDetails(transcriptionEntity)).thenReturn(response);
        when(transcriptionResponseMapper.mapTransactionEntityToTransactionDetails(transcriptionEntity1)).thenReturn(response1);

        List<GetTranscriptionDetailAdminResponse> fndTranscriptions = adminTranscriptionService
            .getTranscriptionsForUser(userId, dateTimeOfSearch);

        assertEquals(transcriptionEntityList.size(), fndTranscriptions.size());
        assertTrue(fndTranscriptions.contains(response));
        assertTrue(fndTranscriptions.contains(response1));
    }

    @Test
    void testGetTranscriptionDetailsNoTranscriptionsFound() {
        Integer userId = 200;
        OffsetDateTime dateTimeOfSearch = OffsetDateTime.now();

        when(transcriptionRepository.findTranscriptionForUserOnOrAfterDate(userId, dateTimeOfSearch))
            .thenReturn(new ArrayList<>());

        List<GetTranscriptionDetailAdminResponse> fndTranscriptions = adminTranscriptionService
            .getTranscriptionsForUser(userId, dateTimeOfSearch);

        assertTrue(fndTranscriptions.isEmpty());
    }

    @Test
    void testGetTranscriptionDetailsUserNotExist() {
        Integer userId = 200;
        OffsetDateTime dateTimeOfSearch = OffsetDateTime.now();

        Mockito.doThrow(new DartsApiException(UserManagementError.USER_NOT_FOUND)).when(userAccountExistsValidator).validate(userId);

        DartsApiException exception = assertThrows(DartsApiException.class, () -> {
            adminTranscriptionService
                .getTranscriptionsForUser(userId, dateTimeOfSearch);
        });
        assertEquals(UserManagementError.USER_NOT_FOUND, exception.getError());
    }

    @Test
    void getTranscriptionDocumentByIdNotExist() {
        Integer transDocId = 200;

        when(transcriptionDocumentRepository.findById(transDocId)).thenReturn(Optional.empty());

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> adminTranscriptionService.getTranscriptionDocumentById(transDocId));

        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND, exception.getError());
    }

    @Test
    void getTranscriptionDocumentById() {
        Integer transDocId = 200;

        TranscriptionDocumentEntity transcriptionDocumentEntity = mock(TranscriptionDocumentEntity.class);
        GetTranscriptionDocumentByIdResponse expectedResponse = new GetTranscriptionDocumentByIdResponse();
        when(transcriptionDocumentRepository.findById(transDocId)).thenReturn(Optional.ofNullable(transcriptionDocumentEntity));
        when(transcriptionResponseMapper.getSearchByTranscriptionDocumentId(transcriptionDocumentEntity)).thenReturn(expectedResponse);

        GetTranscriptionDocumentByIdResponse actualResponse = adminTranscriptionService.getTranscriptionDocumentById(transDocId);

        assertEquals(expectedResponse, actualResponse);
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
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        ObjectHiddenReasonEntity objectHiddenReasonEntity = new ObjectHiddenReasonEntity();
        TranscriptionDocumentHideResponse expectedResponse = new TranscriptionDocumentHideResponse();

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(objectHiddenReasonEntity));

        when(objectAdminActionRepository.saveAndFlush(objectAdminActionEntityArgumentCaptor.capture())).thenReturn(objectAdminActionEntity);

        when(transcriptionResponseMapper.mapHideOrShowResponse(transcriptionDocumentEntity, objectAdminActionEntity)).thenReturn(expectedResponse);

        //run the test
        TranscriptionDocumentHideResponse actualResponse
            = adminTranscriptionService.hideOrShowTranscriptionDocumentById(hideOrShowTranscriptionDocument, request);


        // make the assertion
        assertTrue(transcriptionDocumentEntityArgumentCaptor.getValue().isHidden());
        assertEquals(expectedResponse, actualResponse);
        assertEquals(request.getAdminAction().getComments(), objectAdminActionEntityArgumentCaptor.getValue().getComments());
        assertEquals(request.getAdminAction().getReasonId(), reasonId);
        Assertions.assertFalse(objectAdminActionEntityArgumentCaptor.getValue().isMarkedForManualDeletion());
        assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getHiddenBy());
        assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getHiddenDateTime());
        assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getMarkedForManualDelBy());
        assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getMarkedForManualDelDateTime());
        verify(auditApi).record(HIDE_TRANSCRIPTION);
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
        objectAdminActionEntity.setTicketReference("Ticket-123");
        objectAdminActionEntity.setComments("some comment");

        ObjectAdminActionEntity objectAdminActionEntity1 = new ObjectAdminActionEntity();
        objectAdminActionEntity1.setId(objectAdminActionEntityId1);
        objectAdminActionEntity1.setTicketReference("Ticket-456");
        objectAdminActionEntity1.setComments("some comment 2");

        when(transcriptionDocumentRepository.saveAndFlush(transcriptionDocumentEntityArgumentCaptor.capture())).thenReturn(transcriptionDocumentEntity);
        when(objectAdminActionRepository
                 .findByTranscriptionDocument_Id(hideOrShowTranscriptionDocument)).thenReturn(List.of(objectAdminActionEntity, objectAdminActionEntity1));

        TranscriptionDocumentHideResponse expectedResponse = new TranscriptionDocumentHideResponse();

        when(transcriptionResponseMapper.mapHideOrShowResponse(transcriptionDocumentEntity, null)).thenReturn(expectedResponse);

        // run the test
        TranscriptionDocumentHideResponse actualResponse
            = adminTranscriptionService.hideOrShowTranscriptionDocumentById(hideOrShowTranscriptionDocument, request);

        // make the assertion
        Assertions.assertFalse(transcriptionDocumentEntityArgumentCaptor.getValue().isHidden());
        assertEquals(expectedResponse, actualResponse);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId1);
        verify(auditApi).record(UNHIDE_TRANSCRIPTION, "Ticket reference: Ticket-123, Comments: some comment");
        verify(auditApi).record(UNHIDE_TRANSCRIPTION, "Ticket reference: Ticket-456, Comments: some comment 2");

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
            seed % 2 == 0,
            OffsetDateTime.parse("2021-02-02T00:00:00Z").plusDays(seed));
    }

    @Nested
    class ApproveDeletionTests {

        @Test
        void shouldApproveDeletionWhenDocumentExists() {
            updateManualDeletion(true);
            // Given
            Integer documentId = 1;
            TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
            ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
            objectAdminActionEntity.setId(1);
            UserAccountEntity userAccount = mock(UserAccountEntity.class);
            AdminApproveDeletionResponse expectedResponse = mock(AdminApproveDeletionResponse.class);

            ArgumentCaptor<ObjectAdminActionEntity> objectAdminActionEntityArgumentCaptor = ArgumentCaptor.forClass(ObjectAdminActionEntity.class);

            when(transcriptionDocumentRepository.findById(documentId)).thenReturn(Optional.of(documentEntity));
            when(objectAdminActionRepository
                     .findByTranscriptionDocument_IdAndObjectHiddenReasonIsNotNullAndObjectHiddenReason_MarkedForDeletionTrue(documentId))
                .thenReturn(Optional.of(objectAdminActionEntity));
            when(userIdentity.getUserAccount()).thenReturn(userAccount);
            when(transcriptionResponseMapper.mapAdminApproveDeletionResponse(documentEntity, objectAdminActionEntity))
                .thenReturn(expectedResponse);

            // When
            AdminApproveDeletionResponse actualResponse = adminTranscriptionService.approveDeletionOfTranscriptionDocumentById(documentId);

            // Then
            verify(auditApi).record(eq(AuditActivity.MANUAL_DELETION), notNull(), eq(objectAdminActionEntity.getId().toString()));
            verify(transcriptionApproveMarkForDeletionValidator).validate(documentId);
            verify(objectAdminActionRepository).save(objectAdminActionEntityArgumentCaptor.capture());
            ObjectAdminActionEntity capturedEntity = objectAdminActionEntityArgumentCaptor.getValue();
            assertEquals(expectedResponse, actualResponse);
            assertTrue(capturedEntity.isMarkedForManualDeletion(), "Entity should be marked for manual deletion");
            assertEquals(userAccount, capturedEntity.getMarkedForManualDelBy(), "Entity's deletion should be marked by the correct user");

            assertNotNull(capturedEntity.getMarkedForManualDelDateTime(), "Entity's deletion datetime should be set");
        }

        @Test
        void shouldThrowExceptionWhenDocumentNotFound() {
            updateManualDeletion(true);
            // Given
            Integer documentId = 1;

            when(transcriptionDocumentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When
            DartsApiException exception = assertThrows(DartsApiException.class,
                                                       () -> adminTranscriptionService.approveDeletionOfTranscriptionDocumentById(documentId));

            // Then
            assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND, exception.getError());
        }

        @Test
        void shouldThrowExceptionWhenDeletionNotSupported() {
            updateManualDeletion(true);
            // Given
            Integer documentId = 1;
            TranscriptionDocumentEntity documentEntity = mock(TranscriptionDocumentEntity.class);

            when(transcriptionDocumentRepository.findById(documentId)).thenReturn(Optional.of(documentEntity));
            when(objectAdminActionRepository
                     .findByTranscriptionDocument_IdAndObjectHiddenReasonIsNotNullAndObjectHiddenReason_MarkedForDeletionTrue(documentId))
                .thenReturn(Optional.empty());

            // When
            DartsApiException exception = assertThrows(DartsApiException.class,
                                                       () -> adminTranscriptionService.approveDeletionOfTranscriptionDocumentById(documentId));

            // Then
            assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_DELETE_NOT_SUPPORTED, exception.getError());
        }
    }

    @Test
    void approveDeletionOfTranscriptionDocumentByIdManualDeletionDisabled() {
        updateManualDeletion(false);
        DartsApiException dartsApiException = assertThrows(
            DartsApiException.class, () -> adminTranscriptionService.approveDeletionOfTranscriptionDocumentById(1));
        assertThat(dartsApiException.getError()).isEqualTo(CommonApiError.FEATURE_FLAG_NOT_ENABLED);
    }
}