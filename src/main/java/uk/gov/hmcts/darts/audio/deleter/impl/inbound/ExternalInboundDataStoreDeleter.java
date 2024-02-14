package uk.gov.hmcts.darts.audio.deleter.impl.inbound;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

@Service
public class ExternalInboundDataStoreDeleter extends ExternalDataStoreDeleterImpl<ExternalObjectDirectoryEntity> {


    public ExternalInboundDataStoreDeleter(ObjectRecordStatusRepository objectRecordStatusRepository,
                                           JpaRepository<ExternalObjectDirectoryEntity, Integer> repository,
                                           InboundExternalObjectDirectoryDeletedFinder finder, InboundDataStoreDeleter deleter,
                                           SystemUserHelper systemUserHelper) {
        super(objectRecordStatusRepository, repository, finder, deleter, systemUserHelper);
    }
}
