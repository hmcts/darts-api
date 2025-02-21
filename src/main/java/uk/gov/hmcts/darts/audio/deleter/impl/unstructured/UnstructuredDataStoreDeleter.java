package uk.gov.hmcts.darts.audio.deleter.impl.unstructured;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.DataStoreDeleter;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

@RequiredArgsConstructor
@Service
public class UnstructuredDataStoreDeleter implements DataStoreDeleter {

    private final DataManagementApi dataManagementApi;

    @Override
    public void delete(String location) throws AzureDeleteBlobException {
        dataManagementApi.deleteBlobDataFromUnstructuredContainer(location);
    }
}
