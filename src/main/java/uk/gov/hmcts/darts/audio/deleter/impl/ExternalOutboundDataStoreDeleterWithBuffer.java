package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@Slf4j
public class ExternalOutboundDataStoreDeleterWithBuffer extends ExternalOutboundDataStoreDeleter {

    private final Duration transientObjectDirectoryDeleteBuffer;
    private final CurrentTimeHelper currentTimeHelper;

    public ExternalOutboundDataStoreDeleterWithBuffer(TransientObjectDirectoryRepository repository,
                                                      TransformedMediaRepository transformedMediaRepository,
                                                      DataManagementApi dataManagementApi,
                                                      CurrentTimeHelper currentTimeHelper,
                                                      @Value("${darts.automated.task.external-datastore-deleter.transient-object-directory-delete-buffer}")
                                                      Duration transientObjectDirectoryDeleteBuffer) {
        super(repository, transformedMediaRepository, dataManagementApi);
        this.currentTimeHelper = currentTimeHelper;
        this.transientObjectDirectoryDeleteBuffer = transientObjectDirectoryDeleteBuffer;
    }

    @Override
    protected void deleteFromRepository(TransientObjectDirectoryEntity transientObjectDirectoryEntity) {
        TransformedMediaEntity transformedMedia = transientObjectDirectoryEntity.getTransformedMedia();
        OffsetDateTime maxTimeToDelete = currentTimeHelper.currentOffsetDateTime().minus(transientObjectDirectoryDeleteBuffer);
        if (transformedMedia == null || maxTimeToDelete.isAfter(transformedMedia.getExpiryTime())) {
            super.deleteFromRepository(transientObjectDirectoryEntity);
        }
    }
}
