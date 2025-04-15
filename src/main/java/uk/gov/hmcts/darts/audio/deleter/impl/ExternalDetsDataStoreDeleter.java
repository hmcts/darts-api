package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.AbstractExternalDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.dets.api.DetsDataManagementApi;

import java.util.Collection;

@Service
@Slf4j
public class ExternalDetsDataStoreDeleter extends AbstractExternalDataStoreDeleter<ExternalObjectDirectoryEntity, ExternalObjectDirectoryRepository> {
    private final DetsDataManagementApi dataManagementApi;

    public ExternalDetsDataStoreDeleter(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                        DetsDataManagementApi dataManagementApi) {
        super(externalObjectDirectoryRepository);
        this.dataManagementApi = dataManagementApi;
    }

    @Override
    public void deleteFromDataStore(String externalLocation) throws AzureDeleteBlobException {
        dataManagementApi.deleteBlobDataFromContainer(externalLocation);
    }

    @Override
    protected Collection<ExternalObjectDirectoryEntity> findItemsToDelete(int batchSize) {
        return getRepository().findByExternalLocationTypeAndObjectStatus(
            EodHelper.detsLocation(),
            EodHelper.markForDeletionStatus(),
            Limit.of(batchSize)
        );
    }
}