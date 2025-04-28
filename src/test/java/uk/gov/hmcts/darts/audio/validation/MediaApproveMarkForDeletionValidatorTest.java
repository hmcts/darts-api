package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.test.common.data.ObjectAdminActionTestData;
import uk.gov.hmcts.darts.test.common.data.ObjectHiddenReasonTestData;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaApproveMarkForDeletionValidatorTest {

    @Mock
    private MediaIdValidator mediaIdValidator;
    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;
    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;
    @Mock
    private UserIdentity userIdentity;

    @InjectMocks
    MediaApproveMarkForDeletionValidator mediaApproveMarkForDeletionValidator;

    @Test
    void validateMediaWhereMediaNotMarkedForDeletion() {
        // given
        Long mediaId = 200L;

        // when
        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> mediaApproveMarkForDeletionValidator.validate(mediaId));

        // then
        assertEquals(AudioApiError.ADMIN_MEDIA_MARKED_FOR_DELETION_NOT_FOUND, exception.getError());
    }

    @Test
    void validateMediaWhereObjectAdminActionHasTooManyResults() {
        // given
        Long mediaId = 200L;
        var objectAdminAction1 = ObjectAdminActionTestData.minimalObjectAdminAction();
        var objectAdminAction2 = ObjectAdminActionTestData.minimalObjectAdminAction();
        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of(objectAdminAction1, objectAdminAction2));


        // when
        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> mediaApproveMarkForDeletionValidator.validate(mediaId));

        // then
        assertEquals(AudioApiError.TOO_MANY_RESULTS, exception.getError());
    }

    @Test
    void validateMediaWhereObjectAdminActionIsAlreadyAuthorised() {
        // given
        Long mediaId = 200L;
        var objectAdminAction = ObjectAdminActionTestData.minimalObjectAdminAction();
        objectAdminAction.setMarkedForManualDeletion(true);
        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of(objectAdminAction));


        // when
        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> mediaApproveMarkForDeletionValidator.validate(mediaId));

        // then
        assertEquals(AudioApiError.MEDIA_ALREADY_MARKED_FOR_DELETION, exception.getError());
    }

    @Test
    void validateMediaWhereObjectAdminActionHasNullHiddenReason() {
        // given
        Long mediaId = 200L;
        var objectAdminAction = ObjectAdminActionTestData.minimalObjectAdminAction();
        objectAdminAction.setMarkedForManualDeletion(false);
        objectAdminAction.setObjectHiddenReason(null);
        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of(objectAdminAction));

        // when
        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> mediaApproveMarkForDeletionValidator.validate(mediaId));

        // then
        assertEquals(AudioApiError.MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND, exception.getError());
    }
    
    @Test
    void validateMediaWhereObjectAdminActionApprovedBySameUserAsHidden() {
        // given
        Long mediaId = 200L;
        var userAccount = UserAccountTestData.minimalUserAccount();
        userAccount.setId(123);
        var objectAdminAction = ObjectAdminActionTestData.minimalObjectAdminAction();
        objectAdminAction.setMarkedForManualDeletion(false);
        objectAdminAction.setHiddenBy(userAccount);

        var hiddenReason = ObjectHiddenReasonTestData.otherDelete();
        objectAdminAction.setObjectHiddenReason(hiddenReason);

        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of(objectAdminAction));
        when(objectHiddenReasonRepository.findById(hiddenReason.getId())).thenReturn(Optional.of(hiddenReason));
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        // when
        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> mediaApproveMarkForDeletionValidator.validate(mediaId));

        // then
        assertEquals(AudioApiError.USER_CANNOT_APPROVE_THEIR_OWN_DELETION, exception.getError());
    }

    @Test
    void validateMediaSuccess() {
        // given
        Long mediaId = 200L;
        var hiddenByUserAccount = UserAccountTestData.minimalUserAccount();
        hiddenByUserAccount.setId(123);
        var objectAdminAction = ObjectAdminActionTestData.minimalObjectAdminAction();
        objectAdminAction.setMarkedForManualDeletion(false);
        objectAdminAction.setHiddenBy(hiddenByUserAccount);

        var hiddenReason = ObjectHiddenReasonTestData.otherDelete();
        objectAdminAction.setObjectHiddenReason(hiddenReason);

        var authorisedByUserAccount = UserAccountTestData.minimalUserAccount();
        authorisedByUserAccount.setId(345);

        when(objectAdminActionRepository.findByMediaId(mediaId)).thenReturn(List.of(objectAdminAction));
        when(objectHiddenReasonRepository.findById(hiddenReason.getId())).thenReturn(Optional.of(hiddenReason));
        when(userIdentity.getUserAccount()).thenReturn(authorisedByUserAccount);

        // when then
        assertDoesNotThrow(() -> mediaApproveMarkForDeletionValidator.validate(mediaId));

    }


}