package uk.gov.hmcts.darts.common.service;

import com.azure.storage.blob.BlobClient;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

public interface TransientObjectDirectoryService {

    TransientObjectDirectoryEntity saveTransientObjectDirectoryEntity(TransformedMediaEntity transformedMediaEntity,
                                                                      BlobClient blobClient);
}
