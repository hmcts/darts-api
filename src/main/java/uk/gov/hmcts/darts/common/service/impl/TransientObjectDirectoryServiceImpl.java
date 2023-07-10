package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccount;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;

import java.util.UUID;

import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.NEW;

@Service
@RequiredArgsConstructor
public class TransientObjectDirectoryServiceImpl implements TransientObjectDirectoryService {

    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;

    @Override
    public TransientObjectDirectoryEntity saveTransientDataLocation(MediaRequestEntity mediaRequest,
                                                                    UUID externalLocation) {

        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setMediaRequest(mediaRequest);
        transientObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(NEW.getId()));
        transientObjectDirectoryEntity.setExternalLocation(externalLocation);
        transientObjectDirectoryEntity.setChecksum(null);
        transientObjectDirectoryEntity.setTransferAttempts(null);
        transientObjectDirectoryEntity.setModifiedBy(new UserAccount());

        return transientObjectDirectoryRepository.saveAndFlush(transientObjectDirectoryEntity);
    }

}
