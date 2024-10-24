package uk.gov.hmcts.darts.audio.deleter.impl.dets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

@Service
public class ExternalDetsDataStoreDeleter extends ExternalDataStoreDeleterImpl<ExternalObjectDirectoryEntity> {

    public ExternalDetsDataStoreDeleter(JpaRepository<ExternalObjectDirectoryEntity, Integer> repository,
                                        DetsExternalObjectDirectoryDeletedFinder finder,
                                        DetsDataStoreDeleter deleter,
                                        TransformedMediaRepository transformedMediaRepository) {
        super(repository, finder, deleter, transformedMediaRepository);
    }

}