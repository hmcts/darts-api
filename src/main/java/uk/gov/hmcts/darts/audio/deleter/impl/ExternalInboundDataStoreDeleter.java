package uk.gov.hmcts.darts.audio.deleter.impl;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.AbstractExternalDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.Collection;

@Service
public class ExternalInboundDataStoreDeleter extends AbstractExternalDataStoreDeleter<ExternalObjectDirectoryEntity, ExternalObjectDirectoryRepository> {
    private final DataManagementApi dataManagementApi;

    public ExternalInboundDataStoreDeleter(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                           DataManagementApi dataManagementApi) {
        super(externalObjectDirectoryRepository);
        this.dataManagementApi = dataManagementApi;
    }

    @Override
    public void deleteFromDataStore(String location) throws AzureDeleteBlobException {
        dataManagementApi.deleteBlobDataFromInboundContainer(location);
    }

    @Override
    protected Collection<ExternalObjectDirectoryEntity> findItemsToDelete(int batchSize) {
        return getRepository().findByExternalLocationTypeAndObjectStatus(
            EodHelper.inboundLocation(),
            EodHelper.markForDeletionStatus(),
            Limit.of(batchSize)
        );
    }
}
