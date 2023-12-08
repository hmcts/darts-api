package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.deleter.ObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;

import java.util.List;

@RequiredArgsConstructor
@Transactional
public class ExternalObjectDirectoryDeletedFinder implements ObjectDirectoryDeletedFinder<ExternalObjectDirectoryEntity> {

    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalLocationTypeEnum locationType;

    @Override
    public List<ExternalObjectDirectoryEntity> findMarkedForDeletion() {
        return externalObjectDirectoryRepository.findByExternalLocationTypeAndMarkedForDeletion(
            externalLocationTypeRepository.getReferenceById(locationType.getId()),
            getMarkedForDeletionStatus()
        );

    }

    private ObjectRecordStatusEntity getMarkedForDeletionStatus() {
        return objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.MARKED_FOR_DELETION.getId());
    }
}
