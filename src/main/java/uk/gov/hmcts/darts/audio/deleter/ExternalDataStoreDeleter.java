package uk.gov.hmcts.darts.audio.deleter;

import java.util.List;

public interface ExternalDataStoreDeleter<T> {

    List<T> delete();

    boolean delete(T entityToBeDeleted);
}
