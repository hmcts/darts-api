package uk.gov.hmcts.darts.arm.service;

import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;
import java.util.Optional;

public interface ExternalObjectDirectoryService {

    List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEods(Pageable pageable);

    boolean hasAllMediaBeenCopiedFromInboundStorage(List<MediaEntity> mediaEntities);
	
    Optional<ExternalObjectDirectoryEntity> eagerLoadExternalObjectDirectory(Integer externalObjectDirectoryId);
}
