package uk.gov.hmcts.darts.audio.deleter.impl.unstructured;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

@Service
public class UnstructuredExternalObjectDirectoryDeletedFinder extends ExternalObjectDirectoryDeletedFinder {

    public UnstructuredExternalObjectDirectoryDeletedFinder(ExternalLocationTypeRepository externalLocationTypeRepository,
                                                            ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                            ObjectRecordStatusRepository objectRecordStatusRepository) {
        super(externalLocationTypeRepository, externalObjectDirectoryRepository, objectRecordStatusRepository, ExternalLocationTypeEnum.UNSTRUCTURED);
    }
}
