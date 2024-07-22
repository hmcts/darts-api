package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.InboundTranscriptionAndAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboundTranscriptionAndAnnotationDeleterProcessorImpl implements InboundTranscriptionAndAnnotationDeleterProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final SystemUserHelper systemUserHelper;

    private final ObjectRecordStatusRepository objectRecordStatusRepository;

    @Value("${darts.data-management.retention-period.inbound.arm-minimum}")
    int hoursInArm;

    public List<Integer> processDeletionIfAfterHours(int batch) {
        return processDeletionIfAfterHours(batch, hoursInArm);
    }

    @Override
    public List<Integer> processDeletionIfAfterHours(int batch, int hourThreshold) {
        List<Integer> armRecordToBeMarkedForDeletion
            = externalObjectDirectoryRepository.findAllInboundArmMediaExceedingHours(Pageable.ofSize(batch), hourThreshold);

        ObjectRecordStatusEntity status = objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());

        log.debug("Identified records to be deleted  {}",  armRecordToBeMarkedForDeletion.stream().map(Object::toString));
        externalObjectDirectoryRepository.updateStatusAndUserOfObjectDirectory(armRecordToBeMarkedForDeletion, status,
                                                                               systemUserHelper.getHousekeepingUser());
        log.debug("Records have been marked as deleted");

        return armRecordToBeMarkedForDeletion;
    }
}