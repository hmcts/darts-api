package uk.gov.hmcts.darts.arm.service;

import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.List;

public interface ExternalObjectDirectoryService {

    List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEODs(Pageable pageable);

}
