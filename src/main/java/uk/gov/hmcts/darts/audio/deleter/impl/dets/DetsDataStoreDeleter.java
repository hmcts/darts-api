package uk.gov.hmcts.darts.audio.deleter.impl.dets;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.DataStoreDeleter;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.dets.api.DetsDataManagementApi;

@RequiredArgsConstructor
@Service
public class DetsDataStoreDeleter implements DataStoreDeleter {

    private final DetsDataManagementApi dataManagementApi;

    @Override
    public void delete(String location) throws AzureDeleteBlobException {
        dataManagementApi.deleteBlobDataFromContainer(location);
    }
}