package uk.gov.hmcts.darts.arm.service;

import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ExternalObjectDirectoryService {

    List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEods(Pageable pageable);

    boolean hasAllMediaBeenCopiedFromInboundStorage(List<MediaEntity> mediaEntities);
	
    Optional<ExternalObjectDirectoryEntity> eagerLoadExternalObjectDirectory(Integer externalObjectDirectoryId);

    void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToUpdate, OffsetDateTime timestamp);
}
