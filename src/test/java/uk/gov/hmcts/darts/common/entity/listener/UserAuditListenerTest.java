package uk.gov.hmcts.darts.common.entity.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.common.entity.base.LastModifiedBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuditListenerTest {

    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @Mock
    private UserIdentity userIdentity;

    private UserAuditListener userAuditListener;


    @BeforeEach
    void beforeEach() {
        userAuditListener = spy(new UserAuditListener(clock, userIdentity));
    }

    private UserAccountEntity mockUserAccount() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        return userAccount;
    }

    @Test
    void beforeSave_shouldUpdateCreatedByAndModifiedBy_whenUserAccountIsPresent() {
        UserAccountEntity userAccount = mockUserAccount();
        doNothing().when(userAuditListener).updateCreatedBy(any(), any());
        doNothing().when(userAuditListener).updateModifiedBy(any(), any());

        CreatedBy createdBy = mock(CreatedBy.class);
        userAuditListener.beforeSave(createdBy);

        verify(userAuditListener).updateCreatedBy(createdBy, userAccount);
        verify(userAuditListener).updateModifiedBy(createdBy, userAccount);
        verify(userIdentity).getUserAccount();
    }

    @Test
    void beforeSave_shouldSkipAudit_whenUserAccountIsNotPresent() {
        CreatedBy createdBy = mock(CreatedBy.class);
        userAuditListener.beforeSave(createdBy);
        verify(userAuditListener, never()).updateCreatedBy(any(), any());
        verify(userAuditListener, never()).updateModifiedBy(any(), any());
        verify(userIdentity).getUserAccount();
    }

    @Test
    void beforeUpdate_shouldUpdateModifiedBy_whenUserAccountIsPresent() {
        UserAccountEntity userAccount = mockUserAccount();
        doNothing().when(userAuditListener).updateModifiedBy(any(), any());

        CreatedBy createdBy = mock(CreatedBy.class);
        userAuditListener.beforeUpdate(createdBy);

        verify(userAuditListener, never()).updateCreatedBy(any(), any());
        verify(userAuditListener).updateModifiedBy(createdBy, userAccount);
        verify(userIdentity).getUserAccount();
    }

    @Test
    void beforeUpdate_shouldSkipAudit_whenUserAccountIsNotPresent() {
        CreatedBy createdBy = mock(CreatedBy.class);
        userAuditListener.beforeUpdate(createdBy);
        verify(userAuditListener, never()).updateCreatedBy(any(), any());
        verify(userAuditListener, never()).updateModifiedBy(any(), any());
        verify(userIdentity).getUserAccount();
    }

    @Test
    void getUserAccount_shouldReturnUserAccount_whenUserAccountIsPresent() {
        UserAccountEntity mockUserAccount = mockUserAccount();

        Optional<UserAccountEntity> userAccount = userAuditListener.getUserAccount();
        assertThat(userAccount.isPresent()).isTrue();
        assertThat(userAccount.get()).isEqualTo(mockUserAccount);
        verify(userIdentity).getUserAccount();
    }

    @Test
    void getUserAccount_shouldReturnEmpty_whenUserAccountIsNotPresent() {
        Optional<UserAccountEntity> userAccount = userAuditListener.getUserAccount();
        assertThat(userAccount.isPresent()).isFalse();
        verify(userIdentity).getUserAccount();
    }

    @Test
    void updateCreatedBy_shouldUpdateCreatedBy_whenEntityIsCreatedByAndSkipUserAuditIsFalseAndCreatedByIsNull() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        CreatedBy createdBy = mock(CreatedBy.class);
        when(createdBy.isSkipUserAudit()).thenReturn(false);
        when(createdBy.getCreatedBy()).thenReturn(null);

        userAuditListener.updateCreatedBy(createdBy, userAccount);
        verify(createdBy).setCreatedBy(userAccount);
        verify(createdBy).setCreatedDateTime(OffsetDateTime.now(clock));
    }

    @Test
    void updateCreatedBy_shouldSkipAudit_whenEntityIsCreatedByAndSkipUserAuditIsTrue() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        CreatedBy createdBy = mock(CreatedBy.class);
        when(createdBy.isSkipUserAudit()).thenReturn(true);

        userAuditListener.updateCreatedBy(createdBy, userAccount);
        verify(createdBy, never()).setCreatedBy(any());
        verify(createdBy, never()).setCreatedDateTime(any());
    }

    @Test
    void updateCreatedBy_shouldSkipAudit_whenEntityIsCreatedByAndCreatedByIsNotNull() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        CreatedBy createdBy = mock(CreatedBy.class);
        when(createdBy.isSkipUserAudit()).thenReturn(false);
        when(createdBy.getCreatedBy()).thenReturn(mock(UserAccountEntity.class));

        userAuditListener.updateCreatedBy(createdBy, userAccount);
        verify(createdBy, never()).setCreatedBy(any());
        verify(createdBy, never()).setCreatedDateTime(any());
    }

    @Test
    void updateModifiedBy_shouldUpdateModifiedBy_whenEntityIsLastModifiedByAndSkipUserAuditIsFalse() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        LastModifiedBy lastModifiedBy = mock(LastModifiedBy.class);
        when(lastModifiedBy.isSkipUserAudit()).thenReturn(false);

        userAuditListener.updateModifiedBy(lastModifiedBy, userAccount);
        verify(lastModifiedBy).setLastModifiedBy(userAccount);
        verify(lastModifiedBy).setLastModifiedDateTime(OffsetDateTime.now(clock));
    }

    @Test
    void updateModifiedBy_shouldSkipAudit_whenEntityIsLastModifiedByAndSkipUserAuditIsTrue() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        LastModifiedBy lastModifiedBy = mock(LastModifiedBy.class);
        when(lastModifiedBy.isSkipUserAudit()).thenReturn(true);

        userAuditListener.updateModifiedBy(lastModifiedBy, userAccount);
        verify(lastModifiedBy, never()).setLastModifiedBy(any());
        verify(lastModifiedBy, never()).setLastModifiedDateTime(any());
    }
}
