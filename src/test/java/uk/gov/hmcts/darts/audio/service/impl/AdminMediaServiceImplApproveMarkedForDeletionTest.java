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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
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
    void testAdminApproveMediaMarkedForDeletionSuccess() {
        // given
        Integer mediaId = 1;
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(mediaId);

        var hiddenByUserAccount = UserAccountTestData.minimalUserAccount();
        hiddenByUserAccount.setId(123);
        var objectAdminAction = ObjectAdminActionTestData.minimalObjectAdminAction();
        objectAdminAction.setMedia(mediaEntity);
        objectAdminAction.setMarkedForManualDeletion(false);
        objectAdminAction.setHiddenBy(hiddenByUserAccount);
        objectAdminAction.setId(1);
        var hiddenReason = ObjectHiddenReasonTestData.otherDelete();
        objectAdminAction.setObjectHiddenReason(hiddenReason);

        var authorisedByUserAccount = UserAccountTestData.minimalUserAccount();
        authorisedByUserAccount.setId(345);

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(mediaEntity));
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        when(objectAdminActionRepository.findByMedia_Id(mediaId)).thenReturn(List.of(objectAdminAction));
        when(userIdentity.getUserAccount()).thenReturn(authorisedByUserAccount);

        MediaApproveMarkedForDeletionResponse response = adminMediaService.adminApproveMediaMarkedForDeletion(mediaId);

        verify(auditApi).record(eq(AuditActivity.MANUAL_DELETION), notNull(), eq(objectAdminAction.getId().toString()));

        assertNotNull(response);
        verify(mediaApproveMarkForDeletionValidator, times(1)).validate(mediaId);
        verify(objectAdminActionRepository, times(1)).save(objectAdminAction);
    }

    @Test
    void testAdminApproveMediaMarkedForDeletionMediaNotFound() {
        // given
        Integer mediaId = 1;

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        // when
        DartsApiException exception = assertThrows(DartsApiException.class, () -> adminMediaService.adminApproveMediaMarkedForDeletion(mediaId));

        //then
        assertEquals(AudioApiError.MEDIA_NOT_FOUND, exception.getError());
        verify(mediaApproveMarkForDeletionValidator, times(1)).validate(mediaId);
        verify(objectAdminActionRepository, never()).save(any());
    }

}