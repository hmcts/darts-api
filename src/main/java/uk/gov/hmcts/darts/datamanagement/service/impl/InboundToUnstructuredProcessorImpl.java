package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessorSingleElement;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_INGESTION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_SIZE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_TYPE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;


@Service
@RequiredArgsConstructor
@Slf4j
public class InboundToUnstructuredProcessorImpl implements InboundToUnstructuredProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final InboundToUnstructuredProcessorSingleElement singleElementProcessor;
    
    public static final List<Integer> FAILURE_STATES_LIST =
        List.of(
            FAILURE.getId(),
            FAILURE_FILE_NOT_FOUND.getId(),
            FAILURE_FILE_SIZE_CHECK_FAILED.getId(),
            FAILURE_FILE_TYPE_CHECK_FAILED.getId(),
            FAILURE_CHECKSUM_FAILED.getId(),
            FAILURE_ARM_INGESTION_FAILED.getId()
        );

    @Override
    public void processInboundToUnstructured() {
        log.debug("Processing Inbound data store");
        processAllStoredInboundExternalObjects();
    }

    private void processAllStoredInboundExternalObjects() {

        List<ExternalObjectDirectoryEntity> inboundList = externalObjectDirectoryRepository.findByStatusAndType(getStatus(STORED), getType(INBOUND));
        log.info("Found {} EODs in inbound with state STORED to be processed", inboundList.size());

        List<ExternalObjectDirectoryEntity> unstructuredStoredList = externalObjectDirectoryRepository.findByStatusAndType(
            getStatus(STORED), getType(UNSTRUCTURED));
        List<ExternalObjectDirectoryEntity> unstructuredFailedList = externalObjectDirectoryRepository.findByStatusIdInAndType(
            FAILURE_STATES_LIST, getType(UNSTRUCTURED));

        for (ExternalObjectDirectoryEntity inboundExternalObjectDirectory : inboundList) {
            singleElementProcessor.processSingleElement(inboundExternalObjectDirectory, unstructuredStoredList, unstructuredFailedList);
        }
    }

    private ObjectRecordStatusEntity getStatus(ObjectRecordStatusEnum status) {
        return objectRecordStatusRepository.getReferenceById(status.getId());
    }

    private ExternalLocationTypeEntity getType(ExternalLocationTypeEnum type) {
        return externalLocationTypeRepository.getReferenceById(type.getId());
    }
}
