package uk.gov.hmcts.darts.audio.deleter.impl.inbound;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

@Service
public class ExternalInboundDataStoreDeleter extends ExternalDataStoreDeleterImpl<ExternalObjectDirectoryEntity> {

    public ExternalInboundDataStoreDeleter(JpaRepository<ExternalObjectDirectoryEntity, Integer> repository,
                                           InboundExternalObjectDirectoryDeletedFinder finder,
                                           InboundDataStoreDeleter deleter,
                                           TransformedMediaRepository transformedMediaRepository) {
        super(repository, finder, deleter, transformedMediaRepository);
    }

}
