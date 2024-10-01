package uk.gov.hmcts.darts.dets.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.dets.service.ObjectStateRecordService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectStateRecordServiceImpl implements ObjectStateRecordService {

    private final ObjectStateRecordRepository objectStateRecordRepository;

    @Override
    public ObjectStateRecordEntity getObjectStateRecordEntityById(Long objectStateRecordId) {
        Optional<ObjectStateRecordEntity> objectStateRecord = objectStateRecordRepository.findById(objectStateRecordId);
        if (objectStateRecord.isEmpty()) {
            log.error("ObjectStateRecordEntity with id {} not found", objectStateRecordId);
            throw new IllegalArgumentException("ObjectStateRecordEntity with id " + objectStateRecordId + " not found");
        }
        return objectStateRecord.get();
    }
}
