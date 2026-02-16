package uk.gov.hmcts.darts.audio.deleter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.ObjectDirectory;

@Service
public class ExternalDataStoreEntityDeleter {
    @Transactional
    public <T extends ObjectDirectory> boolean deleteEntity(AbstractExternalDataStoreDeleter<T, ?> deleter, T entity) {
        return deleter.deleteInternal(entity);
    }
}

