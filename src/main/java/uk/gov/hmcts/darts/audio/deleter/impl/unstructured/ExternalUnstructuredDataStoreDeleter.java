package uk.gov.hmcts.darts.audio.deleter.impl.unstructured;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

@Service
public class ExternalUnstructuredDataStoreDeleter extends ExternalDataStoreDeleterImpl<ExternalObjectDirectoryEntity> {

    public ExternalUnstructuredDataStoreDeleter(ObjectRecordStatusRepository objectRecordStatusRepository, UserAccountRepository userAccountRepository,
                                                JpaRepository<ExternalObjectDirectoryEntity, Integer> repository,
                                                UnstructuredExternalObjectDirectoryDeletedFinder finder, UnstructuredDataStoreDeleter deleter,
                                                SystemUserHelper systemUserHelper) {
        super(objectRecordStatusRepository, userAccountRepository, repository, finder, deleter, systemUserHelper);
    }
}
