package uk.gov.hmcts.darts.audio.deleter;

import java.util.List;

public interface ExternalDataStoreDeleter<T> {

    List<T> delete(Integer batchSize);

    boolean delete(T entityToBeDeleted);

    List<T> deleteTod(Integer batchSize);

    boolean deleteTod(T entityToBeDeleted, boolean forceDelete);


}
