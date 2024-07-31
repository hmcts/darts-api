package uk.gov.hmcts.darts.audio.auditing;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.event.spi.EnversPostUpdateEventListenerImpl;
import org.hibernate.event.spi.PostUpdateEvent;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

import static uk.gov.hmcts.darts.audio.entity.MediaRequestEntity_.CURRENT_OWNER;

public class DartsEnversPostUpdateEventListener extends EnversPostUpdateEventListenerImpl {

    private final AuditExecutor auditExecutor;

    public DartsEnversPostUpdateEventListener(EnversService enversService, AuditExecutor auditExecutor) {
        super(enversService);
        this.auditExecutor = auditExecutor;
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (event.getEntity() instanceof MediaRequestEntity) {
            int fieldIndex = getFieldIndex(event.getPersister().getPropertyNames(), CURRENT_OWNER);
            var oldValue = event.getOldState()[fieldIndex];
            var newValue = event.getState()[fieldIndex];
            if ((oldValue != null && !oldValue.equals(newValue)) || (oldValue == null && newValue != null)) {
                auditExecutor.run(() -> super.onPostUpdate(event));
            }
        } else {
            super.onPostUpdate(event);
        }
    }

    private int getFieldIndex(String[] propertyNames, String fieldName) {
        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(fieldName)) {
                return i;
            }
        }
        return -1;
    }
}