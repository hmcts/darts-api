package uk.gov.hmcts.darts.audio.deleter.impl;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.AbstractExternalDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.Collection;

@Service
public class ExternalUnstructuredDataStoreDeleter extends AbstractExternalDataStoreDeleter<ExternalObjectDirectoryEntity, ExternalObjectDirectoryRepository> {
    private final DataManagementApi dataManagementApi;

    public ExternalUnstructuredDataStoreDeleter(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                DataManagementApi dataManagementApi) {
        super(externalObjectDirectoryRepository);
        this.dataManagementApi = dataManagementApi;
    }

    @Override
    protected void deleteFromDataStore(String externalLocation) throws Exception {
        dataManagementApi.deleteBlobDataFromUnstructuredContainer(externalLocation);
    }

    @Override
    protected Collection<ExternalObjectDirectoryEntity> findItemsToDelete(int batchSize) {
        return getRepository().findByExternalLocationTypeAndObjectStatus(
            EodHelper.unstructuredLocation(),
            EodHelper.markForDeletionStatus(),
            Limit.of(batchSize)
        );
    }
}
