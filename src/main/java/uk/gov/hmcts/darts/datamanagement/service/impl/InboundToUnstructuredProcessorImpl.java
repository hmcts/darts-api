package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
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
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_EMPTY_FILE;
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
    private final AuditApi auditApi;

    @Value("${darts.data-management.inbound-to-unstructured-limit: 100}")
    private Integer limit;

    public static final List<Integer> FAILURE_STATES_LIST =
        List.of(
            FAILURE.getId(),
            FAILURE_FILE_NOT_FOUND.getId(),
            FAILURE_FILE_SIZE_CHECK_FAILED.getId(),
            FAILURE_FILE_TYPE_CHECK_FAILED.getId(),
            FAILURE_CHECKSUM_FAILED.getId(),
            FAILURE_ARM_INGESTION_FAILED.getId(),
            FAILURE_EMPTY_FILE.getId()
        );

    @Override
    public void processInboundToUnstructured() {
        log.debug("Processing Inbound data store");
        processAllStoredInboundExternalObjectsOneCall();
    }

    private void processAllStoredInboundExternalObjectsOneCall() {
        var system = new UserAccountEntity();
        system.setId(0);
        try {
            log.info("BEFORE SLEEP");
            auditApi.recordAudit(AuditActivity.MOVE_COURTROOM, system, null);
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("AFTER SLEEP");
        auditApi.recordAudit(AuditActivity.APPLY_RETENTION, system, null);

        List<Integer> inboundList = externalObjectDirectoryRepository.findEodIdsForTransfer(getStatus(STORED), getType(INBOUND),
                                                                                            getStatus(STORED), getType(UNSTRUCTURED), 3, limit);
        int count = 1;
        for (Integer inboundObjectId : inboundList) {
            log.debug("Processing Inbound to Unstructured record {} of {} with EOD {}", count++, inboundList.size(), inboundObjectId);
            try {
                singleElementProcessor.processSingleElement(inboundObjectId);
            } catch (Exception exception) {
                log.error("Failed to move from inbound file to unstructured data store for EOD id: {}", inboundObjectId, exception);
            }
        }
    }

    private ObjectRecordStatusEntity getStatus(ObjectRecordStatusEnum status) {
        return objectRecordStatusRepository.getReferenceById(status.getId());
    }

    private ExternalLocationTypeEntity getType(ExternalLocationTypeEnum type) {
        return externalLocationTypeRepository.getReferenceById(type.getId());
    }
}
