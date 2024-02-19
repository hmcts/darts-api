package uk.gov.hmcts.darts.audio.deleter.impl.outbound;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

@Service
public class ExternalOutboundDataStoreDeleter extends ExternalDataStoreDeleterImpl<TransientObjectDirectoryEntity> {


    public ExternalOutboundDataStoreDeleter(ObjectRecordStatusRepository objectRecordStatusRepository,
                                            TransientObjectDirectoryRepository repository,
                                            OutboundExternalObjectDirectoryDeletedFinder finder, OutboundDataStoreDeleter
                                                deleter, SystemUserHelper systemUserHelper,
                                            TransformedMediaRepository transformedMediaRepository) {
        super(objectRecordStatusRepository, repository, finder, deleter, systemUserHelper, transformedMediaRepository);
    }
}
