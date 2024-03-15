package uk.gov.hmcts.darts.audio.deleter.impl.unstructured;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

@Service
public class ExternalUnstructuredDataStoreDeleter extends ExternalDataStoreDeleterImpl<ExternalObjectDirectoryEntity> {

    public ExternalUnstructuredDataStoreDeleter(JpaRepository<ExternalObjectDirectoryEntity, Integer> repository,
                                                UnstructuredExternalObjectDirectoryDeletedFinder finder,
                                                UnstructuredDataStoreDeleter deleter,
                                                TransformedMediaRepository transformedMediaRepository) {
        super(repository, finder, deleter, transformedMediaRepository);
    }

}
