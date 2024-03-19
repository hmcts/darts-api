package uk.gov.hmcts.darts.arm.service.impl;

import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

public abstract class AbstractUnstructuredToArmProcessor implements UnstructuredToArmProcessor {

    protected final ObjectRecordStatusRepository objectRecordStatusRepository;
    protected final UserIdentity userIdentity;
    protected final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    protected final ExternalLocationTypeRepository externalLocationTypeRepository;

    public AbstractUnstructuredToArmProcessor(ObjectRecordStatusRepository objectRecordStatusRepository,
                                              UserIdentity userIdentity,
                                              ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                              ExternalLocationTypeRepository externalLocationTypeRepository) {
        this.objectRecordStatusRepository = objectRecordStatusRepository;
        this.userIdentity = userIdentity;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.externalLocationTypeRepository = externalLocationTypeRepository;
    }

}
