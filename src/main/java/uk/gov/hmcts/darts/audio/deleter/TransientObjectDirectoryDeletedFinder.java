package uk.gov.hmcts.darts.audio.deleter;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.util.List;

@RequiredArgsConstructor
public class TransientObjectDirectoryDeletedFinder implements ObjectDirectoryDeletedFinder<TransientObjectDirectoryEntity> {
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;

    @Override
    public List<TransientObjectDirectoryEntity> findMarkedForDeletion() {
        return transientObjectDirectoryRepository.findByStatus(getMarkedForDeletionStatus());
    }

    private ObjectRecordStatusEntity getMarkedForDeletionStatus() {
        return objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.MARKED_FOR_DELETION.getId());
    }
}
