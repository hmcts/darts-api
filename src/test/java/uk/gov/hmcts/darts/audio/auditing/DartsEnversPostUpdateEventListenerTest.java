package uk.gov.hmcts.darts.audio.auditing;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DartsEnversPostUpdateEventListenerTest {

    private DartsEnversPostUpdateEventListener dartsEnversPostUpdateEventListener;

    @Mock
    private EnversService enversService;

    @Mock
    private EntityPersister entityPersister;

    @Mock
    private AuditExecutor auditExecutor;

    @BeforeEach
    void setUp() {
        dartsEnversPostUpdateEventListener = new DartsEnversPostUpdateEventListener(enversService, auditExecutor);
    }

    @Test
    void invokesAuditingWhenCurrentOwnerHasChanged() {
        when(entityPersister.getPropertyNames()).thenReturn(new String[]{"currentOwner", "requestor"});

        dartsEnversPostUpdateEventListener.onPostUpdate(withDifferentOldAndNewCurrentOwners());

        verify(auditExecutor).run(any(Runnable.class));
    }

    @Test
    void invokesAuditingWhenCurrentOwnerNullAndNewOwnerNotNull() {
        when(entityPersister.getPropertyNames()).thenReturn(new String[]{"currentOwner", "requestor"});

        dartsEnversPostUpdateEventListener.onPostUpdate(withOldOwnerNullAndNewOwnerNotNull());

        verify(auditExecutor).run(any(Runnable.class));
    }

    @Test
    void doesNotInvokeAuditingWhenCurrentOwnerNotUpdated() {
        when(entityPersister.getPropertyNames()).thenReturn(new String[]{"currentOwner", "requestor"});

        dartsEnversPostUpdateEventListener.onPostUpdate(withUnchangedCurrentOwnersButDifferentRequestors());

        verifyNoInteractions(auditExecutor);
    }

    private PostUpdateEvent withDifferentOldAndNewCurrentOwners() {
        var initialOwner = new UserAccountEntity();
        var newOwner = new UserAccountEntity();
        var initialRequestor = new UserAccountEntity();
        var newRequestor = new UserAccountEntity();
        return new PostUpdateEvent(
            new MediaRequestEntity(),
            1,
            new Object[]{newOwner, newRequestor},
            new Object[]{initialOwner, initialRequestor},
            new int[]{},
            entityPersister,
            null
        );
    }

    private PostUpdateEvent withOldOwnerNullAndNewOwnerNotNull() {
        UserAccountEntity initialOwner = null;
        var newOwner = new UserAccountEntity();
        var initialRequestor = new UserAccountEntity();
        var newRequestor = new UserAccountEntity();
        return new PostUpdateEvent(
            new MediaRequestEntity(),
            1,
            new Object[]{newOwner, newRequestor},
            new Object[]{initialOwner, initialRequestor},
            new int[]{},
            entityPersister,
            null
        );
    }

    private PostUpdateEvent withUnchangedCurrentOwnersButDifferentRequestors() {
        var initialOwner = new UserAccountEntity();
        var initialRequestor = new UserAccountEntity();
        var newRequestor = new UserAccountEntity();
        return new PostUpdateEvent(
            new MediaRequestEntity(),
            1,
            new Object[]{initialOwner, newRequestor},
            new Object[]{initialOwner, initialRequestor},
            new int[]{},
            entityPersister,
            null
        );
    }
}