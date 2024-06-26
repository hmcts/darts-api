package uk.gov.hmcts.darts.arm.service;

import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalObjectDirectoryService {

    List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEods(Pageable pageable);

    boolean hasAllMediaBeenCopiedFromInboundStorage(List<MediaEntity> mediaEntities);

    Optional<ExternalObjectDirectoryEntity> eagerLoadExternalObjectDirectory(Integer externalObjectDirectoryId);

    void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToUpdate, OffsetDateTime timestamp);

    ExternalObjectDirectoryEntity createAndSaveExternalObjectDirectory(UUID externalLocation,
                                                                       String checksum,
                                                                       UserAccountEntity userAccountEntity,
                                                                       CaseDocumentEntity caseDocumentEntity,
                                                                       ExternalLocationTypeEntity externalLocationType);
}
