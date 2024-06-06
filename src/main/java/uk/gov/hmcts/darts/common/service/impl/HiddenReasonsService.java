package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.mapper.HiddenReasonMapper;
import uk.gov.hmcts.darts.common.model.HiddenReason;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HiddenReasonsService {

    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;
    private final HiddenReasonMapper hiddenReasonMapper;

    @Transactional(readOnly = true)
    public List<HiddenReason> getHiddenReasons() {
        List<ObjectHiddenReasonEntity> hiddenReasonEntities = objectHiddenReasonRepository.findAll().stream()
            .sorted(Comparator.comparing(ObjectHiddenReasonEntity::getDisplayOrder))
            .collect(Collectors.toList());

        return hiddenReasonMapper.mapToApiModel(hiddenReasonEntities);
    }

}
