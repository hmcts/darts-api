package uk.gov.hmcts.darts.audio.deleter;

import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

public interface DataStoreDeleter {
    void delete(String location) throws AzureDeleteBlobException;
}
