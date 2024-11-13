package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.audio.deleter.ObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.util.List;

@RequiredArgsConstructor
public class TransientObjectDirectoryDeletedFinder implements ObjectDirectoryDeletedFinder<TransientObjectDirectoryEntity> {
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;

    @Override
    public List<TransientObjectDirectoryEntity> findMarkedForDeletion(Integer batchSize) {
        return transientObjectDirectoryRepository.findByStatus(getMarkedForDeletionStatus(), Limit.of(batchSize));
    }

    private ObjectRecordStatusEntity getMarkedForDeletionStatus() {
        return objectRecordStatusRepository.getReferenceById(
            ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());
    }
}
