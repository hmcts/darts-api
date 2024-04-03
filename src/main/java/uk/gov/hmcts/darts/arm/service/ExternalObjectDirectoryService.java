package uk.gov.hmcts.darts.arm.service;

import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

public interface ExternalObjectDirectoryService {

    List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEods(Pageable pageable);

    boolean hasNotAllMediaBeenCopiedFromInboundStorageForAtsProcessing(List<MediaEntity> mediaRequestFiles);
}
