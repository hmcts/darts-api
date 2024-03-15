package uk.gov.hmcts.darts.audio.deleter.impl.outbound;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

@Service
public class ExternalOutboundDataStoreDeleter extends ExternalDataStoreDeleterImpl<TransientObjectDirectoryEntity> {

    public ExternalOutboundDataStoreDeleter(TransientObjectDirectoryRepository repository,
                                            OutboundExternalObjectDirectoryDeletedFinder finder,
                                            OutboundDataStoreDeleter deleter,
                                            TransformedMediaRepository transformedMediaRepository) {
        super(repository, finder, deleter, transformedMediaRepository);
    }

}
