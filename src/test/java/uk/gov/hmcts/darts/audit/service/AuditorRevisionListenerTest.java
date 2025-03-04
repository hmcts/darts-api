package uk.gov.hmcts.darts.audit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.model.RevisionInfo;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditorRevisionListenerTest {

    @Mock
    private UserIdentity userIdentity;

    @InjectMocks
    private AuditorRevisionListener auditorRevisionListener;

    @Test
    void getUserAccount_userIsPresent_shouldReturnUser() {
        int userId = 123;
        when(userIdentity.getUserIdFromJwt()).thenReturn(Optional.of(userId));

        Optional<Integer> result = auditorRevisionListener.getUserAccount();
        assertTrue(result.isPresent());
        assertEquals(userId, result.get());
        verify(userIdentity).getUserIdFromJwt();
    }

    @Test
    void getUserAccount_userNotIsPresent_shouldReturnEmptyOptional() {
        when(userIdentity.getUserIdFromJwt()).thenReturn(Optional.empty());

        assertTrue(auditorRevisionListener.getUserAccount().isEmpty());
        verify(userIdentity).getUserIdFromJwt();

    }

    @Test
    void newRevision_revisionEntityIsInstanceOfRevisionInfoAndUserIsFound_shouldSetAuditUser() {
        int userId = 123;
        when(userIdentity.getUserIdFromJwt()).thenReturn(Optional.of(userId));
        RevisionInfo revisionInfo = mock(RevisionInfo.class);

        auditorRevisionListener.newRevision(revisionInfo);

        verify(revisionInfo).setAuditUser(userId);
        verify(userIdentity).getUserIdFromJwt();
    }

    @Test
    void newRevision_revisionEntityIsInstanceOfRevisionInfoButUserNotFound_shouldNotSetAuditUser() {
        when(userIdentity.getUserIdFromJwt()).thenReturn(Optional.empty());
        RevisionInfo revisionInfo = mock(RevisionInfo.class);

        auditorRevisionListener.newRevision(revisionInfo);

        verify(revisionInfo, never()).setAuditUser(any());
        verify(userIdentity).getUserIdFromJwt();
    }

    @Test
    void newRevision_revisionEntityIsNotInstanceOfRevisionInfo_shouldNotSetAuditUser() {
        Object revisionEntity = new Object();
        auditorRevisionListener.newRevision(revisionEntity);
        verify(userIdentity, never()).getUserIdFromJwt();
    }
}
