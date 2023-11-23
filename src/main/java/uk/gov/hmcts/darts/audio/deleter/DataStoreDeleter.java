package uk.gov.hmcts.darts.audio.deleter;

import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.util.UUID;

public interface DataStoreDeleter {
    void delete(UUID location) throws AzureDeleteBlobException;
}
