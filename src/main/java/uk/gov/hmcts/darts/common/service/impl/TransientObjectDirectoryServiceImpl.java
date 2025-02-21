package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
public class TransientObjectDirectoryServiceImpl implements TransientObjectDirectoryService {

    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public TransientObjectDirectoryEntity saveTransientObjectDirectoryEntity(TransformedMediaEntity transformedMediaEntity,
                                                                             String blobName) {

        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setTransformedMedia(transformedMediaEntity);
        transientObjectDirectoryEntity.setStatus(objectRecordStatusRepository.getReferenceById(STORED.getId()));
        transientObjectDirectoryEntity.setExternalLocation(blobName);
        transientObjectDirectoryEntity.setTransferAttempts(null);
        var systemUser = userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
        transientObjectDirectoryEntity.setCreatedBy(systemUser);
        transientObjectDirectoryEntity.setLastModifiedBy(systemUser);

        return transientObjectDirectoryRepository.saveAndFlush(transientObjectDirectoryEntity);
    }

}
