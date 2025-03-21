package uk.gov.hmcts.darts.common.service.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.mapper.HiddenReasonMapper;
import uk.gov.hmcts.darts.common.model.HiddenReason;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Data
public class HiddenReasonsService {

    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;
    private final HiddenReasonMapper hiddenReasonMapper;

    @Value("${darts.manual-deletion.enabled:false}")
    private final boolean manualDeletionEnabled;

    @Transactional(readOnly = true)
    public List<HiddenReason> getHiddenReasons() {
        List<ObjectHiddenReasonEntity> hiddenReasonEntities = objectHiddenReasonRepository.findAll()
            .stream()
            .filter(objectHiddenReasonEntity -> manualDeletionEnabled || !objectHiddenReasonEntity.isMarkedForDeletion())
            .sorted(Comparator.comparing(ObjectHiddenReasonEntity::getDisplayOrder))
            .toList();

        return hiddenReasonMapper.mapToApiModel(hiddenReasonEntities);
    }

}
