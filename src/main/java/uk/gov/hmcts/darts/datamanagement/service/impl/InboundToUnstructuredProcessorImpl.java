package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessorSingleElement;
import uk.gov.hmcts.darts.task.config.InboundToUnstructuredAutomatedTaskConfig;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final InboundToUnstructuredProcessorSingleElement singleElementProcessor;
    private final InboundToUnstructuredAutomatedTaskConfig asyncTaskConfig;


    @Override
    @SuppressWarnings("PMD.DoNotUseThreads")//TODO - refactor to avoid using Thread.sleep() when this is next edited
    public void processInboundToUnstructured(int batchSize) {
        log.debug("Processing Inbound data store");
        List<Integer> inboundList = externalObjectDirectoryRepository.findEodsForTransfer(getStatus(STORED), getType(INBOUND),
                                                                                          getStatus(STORED), getType(UNSTRUCTURED), 3,
                                                                                          Limit.of(batchSize));

        log.info("Found {} records to process from Inbound to Unstructured out of batch size {}", inboundList.size(), batchSize);
        AtomicInteger count = new AtomicInteger(1);

        List<Callable<Void>> tasks = inboundList.stream()
            .map(inboundObjectId -> (Callable<Void>) () -> {
                log.debug("Processing Inbound to Unstructured record {} of {} with EOD id {}",
                          count.getAndIncrement(), inboundList.size(), inboundObjectId);
                processInboundToUnstructured(inboundObjectId);
                return null;
            }).toList();
        try {
            AsyncUtil.invokeAllAwaitTermination(tasks, asyncTaskConfig);
        } catch (InterruptedException e) {
            log.error("Inbound to Unstructured interrupted exception", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Inbound to Unstructured unexpected exception", e);
        }
    }

    private void processInboundToUnstructured(Long inboundObjectId) {
        try {
            singleElementProcessor.processSingleElement(inboundObjectId);
        } catch (Exception exception) {
            log.error("Failed to move from inbound file to unstructured data store for EOD id: {}", inboundObjectId, exception);
        }
    }

    private ObjectRecordStatusEntity getStatus(ObjectRecordStatusEnum status) {
        return objectRecordStatusRepository.getReferenceById(status.getId());
    }

    private ExternalLocationTypeEntity getType(ExternalLocationTypeEnum type) {
        return externalLocationTypeRepository.getReferenceById(type.getId());
    }
}
