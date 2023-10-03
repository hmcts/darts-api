package uk.gov.hmcts.darts.audio.service.impl;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.impl.DataManagementApiImpl;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.model.AudioRequestType.DOWNLOAD;

@ExtendWith(MockitoExtension.class)
class MediaRequestServiceImplTest {

    private static final Integer TEST_REQUESTER = 1234;
    private static final String OFFSET_T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String OFFSET_T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @InjectMocks
    private MediaRequestServiceImpl mediaRequestService;

    @Mock
    private HearingRepository mockHearingRepository;
    @Mock
    private UserAccountRepository mockUserAccountRepository;
    @Mock
    private MediaRequestRepository mockMediaRequestRepository;
    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    @Mock
    private DataManagementApiImpl dataManagementApi;
    @Mock
    private NotificationApi notificationApi;

    private HearingEntity mockHearingEntity;
    private MediaRequestEntity mockMediaRequestEntity;

    @Mock
    private UserAccountEntity mockUserAccountEntity;

    @BeforeEach
    void beforeEach() {

        mockHearingEntity = new HearingEntity();

        mockMediaRequestEntity = new MediaRequestEntity();
        mockMediaRequestEntity.setHearing(mockHearingEntity);
        mockMediaRequestEntity.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        mockMediaRequestEntity.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        mockMediaRequestEntity.setRequestor(mockUserAccountEntity);
        mockMediaRequestEntity.setStatus(OPEN);
        mockMediaRequestEntity.setAttempts(0);
        OffsetDateTime now = OffsetDateTime.now();
        mockMediaRequestEntity.setCreatedDateTime(now);
        mockMediaRequestEntity.setCreatedBy(mockUserAccountEntity);
        mockMediaRequestEntity.setLastModifiedDateTime(now);
        mockMediaRequestEntity.setLastModifiedBy(mockUserAccountEntity);
    }

    @Test
    void whenSavingAudioRequestIsSuccessful() {

        Integer hearingId = 4567;
        mockHearingEntity.setId(hearingId);
        mockMediaRequestEntity.setId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD);

        when(mockHearingRepository.getReferenceById(hearingId)).thenReturn(mockHearingEntity);
        when(mockMediaRequestRepository.saveAndFlush(any(MediaRequestEntity.class))).thenReturn(mockMediaRequestEntity);
        when(mockUserAccountRepository.getReferenceById(TEST_REQUESTER)).thenReturn(mockUserAccountEntity);
        var request = mediaRequestService.saveAudioRequest(requestDetails);

        assertEquals(request.getId(), mockMediaRequestEntity.getId());
        verify(mockHearingRepository).getReferenceById(hearingId);
        verify(mockMediaRequestRepository).saveAndFlush(any(MediaRequestEntity.class));
        verify(mockUserAccountRepository).getReferenceById(TEST_REQUESTER);
    }

    @SneakyThrows
    @Test
    void shouldScheduleRequestPendingNotification() {
        var mockCourtCaseEntity = new CourtCaseEntity();
        mockCourtCaseEntity.setId(1001);
        mockMediaRequestEntity.getHearing().setCourtCase(mockCourtCaseEntity);
        var mockUserAccountEntity = new UserAccountEntity();
        mockUserAccountEntity.setEmailAddress("test@test.com");
        mockMediaRequestEntity.setRequestor(mockUserAccountEntity);
        when(notificationApi.getNotificationTemplateIdByName("audio_request_being_processed")).thenReturn("request_pending_template");

        mediaRequestService.scheduleMediaRequestPendingNotification(mockMediaRequestEntity);

        var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
            .eventId("request_pending_template")
            .caseId(1001)
            .emailAddresses("test@test.com")
            .build();
        verify(notificationApi, Mockito.times(1)).scheduleNotification(eq(saveNotificationToDbRequest));
    }

    @Test
    void whenAudioRequestHasBeenProcessedDeleteBlobDataAndAudioRequest() {
        var mediaRequestId = 1;
        UUID blobId = UUID.randomUUID();

        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(blobId);

        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.of(transientObjectDirectoryEntity));

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(1)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(transientObjectDirectoryRepository, Mockito.times(1)).deleteById(any());
    }

    @Test
    void whenTransientObjectHasNoExternalLocationValueAvoidDeletingFromBlobStorage() {
        var mediaRequestId = 1;
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setExternalLocation(null);

        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.of(transientObjectDirectoryEntity));

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(0)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(transientObjectDirectoryRepository, Mockito.times(1)).deleteById(any());
    }

    @Test
    void whenNoAudioIsPresentOnlyDeleteAudioRequest() {
        var mediaRequestId = 1;
        when(transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId))
            .thenReturn(Optional.empty());

        mediaRequestService.deleteAudioRequest(mediaRequestId);

        verify(mockMediaRequestRepository, Mockito.times(1)).deleteById(eq(mediaRequestId));
        verify(dataManagementApi, Mockito.times(0)).deleteBlobDataFromOutboundContainer(any(UUID.class));
        verify(transientObjectDirectoryRepository, Mockito.times(0)).deleteById(any());
    }

}
