package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.MediaApproveMarkedForDeletionResponse;
import uk.gov.hmcts.darts.audio.validation.MediaApproveMarkForDeletionValidator;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.test.common.data.ObjectAdminActionTestData;
import uk.gov.hmcts.darts.test.common.data.ObjectHiddenReasonTestData;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMediaServiceImplApproveMarkedForDeletionTest {
    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private MediaApproveMarkForDeletionValidator mediaApproveMarkForDeletionValidator;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @InjectMocks
    private AdminMediaServiceImpl adminMediaService;
    @Mock
    private UserAccountEntity userAccount;

    @Mock
    private AuditApi auditApi;

    @BeforeEach
    void setUp() {
        lenient().when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        this.adminMediaService = spy(adminMediaService);
        when(adminMediaService.isManualDeletionEnabled()).thenReturn(true);
    }

    @Test
    void adminApproveMediaMarkedForDeletion_shouldMarkMediaForDeletion_whenUsingTypicalData() {
        // given
        Long mediaId1 = 1L;
        Long mediaId2 = 2L;
        MediaEntity mediaEntity1 = new MediaEntity();
        mediaEntity1.setId(mediaId1);
        mediaEntity1.setChronicleId("123");
        MediaEntity mediaEntity2 = new MediaEntity();
        mediaEntity2.setId(mediaId2);
        mediaEntity2.setChronicleId("123");

        var hiddenByUserAccount = UserAccountTestData.minimalUserAccount();
        hiddenByUserAccount.setId(123);

        var objectAdminAction1 = ObjectAdminActionTestData.minimalObjectAdminAction();
        objectAdminAction1.setMedia(mediaEntity1);
        objectAdminAction1.setMarkedForManualDeletion(false);
        objectAdminAction1.setHiddenBy(hiddenByUserAccount);
        objectAdminAction1.setId(1);
        var hiddenReason = ObjectHiddenReasonTestData.otherDelete();
        objectAdminAction1.setObjectHiddenReason(hiddenReason);
        mediaEntity1.setObjectAdminAction(objectAdminAction1);

        var objectAdminAction2 = ObjectAdminActionTestData.minimalObjectAdminAction();
        objectAdminAction2.setMedia(mediaEntity2);
        objectAdminAction2.setMarkedForManualDeletion(false);
        objectAdminAction2.setHiddenBy(hiddenByUserAccount);
        objectAdminAction2.setId(2);
        var hiddenReason2 = ObjectHiddenReasonTestData.otherDelete();
        objectAdminAction2.setObjectHiddenReason(hiddenReason2);
        mediaEntity2.setObjectAdminAction(objectAdminAction2);

        when(mediaRepository.findByIdIncludeDeleted(mediaId1)).thenReturn(Optional.of(mediaEntity1));
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        when(mediaRepository.findAllByChronicleId("123")).thenReturn(List.of(mediaEntity1,mediaEntity2));

        MediaApproveMarkedForDeletionResponse response = adminMediaService.adminApproveMediaMarkedForDeletion(mediaId1);

        verify(auditApi).record(eq(AuditActivity.MANUAL_DELETION), eq(userAccount), eq(objectAdminAction1.getId().toString()));
        verify(auditApi).record(eq(AuditActivity.MANUAL_DELETION), eq(userAccount), eq(objectAdminAction2.getId().toString()));

        assertNotNull(response);
        verify(mediaApproveMarkForDeletionValidator, times(1)).validate(mediaId1);
        verify(objectAdminActionRepository, times(1)).saveAll(List.of(objectAdminAction1,objectAdminAction2));
        verify(mediaRepository).findAllByChronicleId("123");

        assertThat(objectAdminAction1.isMarkedForManualDeletion()).isTrue();
        assertThat(objectAdminAction1.getMarkedForManualDelBy()).isEqualTo(userAccount);
        assertThat(objectAdminAction2.isMarkedForManualDeletion()).isTrue();
        assertThat(objectAdminAction2.getMarkedForManualDelBy()).isEqualTo(userAccount);

    }

    @Test
    void testAdminApproveMediaMarkedForDeletionMediaNotFound() {
        // given
        Long mediaId = 1L;

        when(mediaRepository.findByIdIncludeDeleted(mediaId)).thenReturn(Optional.empty());

        // when
        DartsApiException exception = assertThrows(DartsApiException.class, () -> adminMediaService.adminApproveMediaMarkedForDeletion(mediaId));

        //then
        assertEquals(AudioApiError.MEDIA_NOT_FOUND, exception.getError());
        verify(mediaApproveMarkForDeletionValidator, times(1)).validate(mediaId);
        verify(objectAdminActionRepository, never()).save(any());
    }

}