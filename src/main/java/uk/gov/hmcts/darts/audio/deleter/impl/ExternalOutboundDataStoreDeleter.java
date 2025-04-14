package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.AbstractExternalDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.Collection;

@Service
@Slf4j
public class ExternalOutboundDataStoreDeleter extends AbstractExternalDataStoreDeleter<TransientObjectDirectoryEntity, TransientObjectDirectoryRepository> {

    private final TransformedMediaRepository transformedMediaRepository;
    private final DataManagementApi dataManagementApi;

    public ExternalOutboundDataStoreDeleter(TransientObjectDirectoryRepository repository,
                                            TransformedMediaRepository transformedMediaRepository,
                                            DataManagementApi dataManagementApi) {
        super(repository);
        this.transformedMediaRepository = transformedMediaRepository;
        this.dataManagementApi = dataManagementApi;
    }

    @Override
    protected void deleteFromDataStore(String externalLocation) throws Exception {
        dataManagementApi.deleteBlobDataFromOutboundContainer(externalLocation);
    }

    @Override
    protected Collection<TransientObjectDirectoryEntity> findItemsToDelete(int batchSize) {
        return getRepository().findByStatus(
            EodHelper.markForDeletionStatus(),
            Limit.of(batchSize));
    }

    @Override
    protected void deleteFromRepository(TransientObjectDirectoryEntity transientObjectDirectoryEntity) {
        super.deleteFromRepository(transientObjectDirectoryEntity);
        if (transientObjectDirectoryEntity.getTransformedMedia() != null) {
            log.debug("Deleting transformed media {} with transient object directory id={}",
                      transientObjectDirectoryEntity.getTransformedMedia().getId(), transientObjectDirectoryEntity.getId());
            transformedMediaRepository.delete(transientObjectDirectoryEntity.getTransformedMedia());
        }
    }
}
