package uk.gov.hmcts.darts.audio.deleter;

import java.util.Collection;

public interface ExternalDataStoreDeleter<T> {

    Collection<T> delete(Integer batchSize);

    boolean delete(T entityToBeDeleted);
}
