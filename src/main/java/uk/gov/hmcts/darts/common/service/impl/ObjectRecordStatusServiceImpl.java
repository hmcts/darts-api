package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.ObjectRecordStatusService;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ObjectRecordStatusServiceImpl implements ObjectRecordStatusService {

    private final ObjectRecordStatusRepository objectRecordStatusRepository;

    @Override
    public ObjectRecordStatusEntity getObjectRecordStatusEntity(ObjectRecordStatusEnum objectRecordStatusEnum) {
        return objectRecordStatusRepository.findById(objectRecordStatusEnum.getId()).orElseThrow(
            () -> new DartsApiException(CommonApiError.NOT_FOUND, "Failed to find Object record with status '" + objectRecordStatusEnum + "'"));
    }
}
