package uk.gov.hmcts.darts.common.service.impl;

import com.azure.storage.blob.BlobClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;

import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@Service
@RequiredArgsConstructor
public class TransientObjectDirectoryServiceImpl implements TransientObjectDirectoryService {

    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public TransientObjectDirectoryEntity saveTransientObjectDirectoryEntity(TransformedMediaEntity transformedMediaEntity,
                                                                             BlobClient blobClient) {

        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setTransformedMedia(transformedMediaEntity);
        transientObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(STORED.getId()));
        transientObjectDirectoryEntity.setExternalLocation(UUID.fromString(blobClient.getBlobName()));
        transientObjectDirectoryEntity.setTransferAttempts(null);
        var systemUser = userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
        transientObjectDirectoryEntity.setCreatedBy(systemUser);
        transientObjectDirectoryEntity.setLastModifiedBy(systemUser);

        return transientObjectDirectoryRepository.saveAndFlush(transientObjectDirectoryEntity);
    }

}
