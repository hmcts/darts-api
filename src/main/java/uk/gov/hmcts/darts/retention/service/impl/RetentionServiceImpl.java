package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.mapper.RetentionMapper;
import uk.gov.hmcts.darts.retention.mapper.RetentionPolicyMapper;
import uk.gov.hmcts.darts.retention.service.RetentionService;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;
import uk.gov.hmcts.darts.retentions.model.GetRetentionPolicy;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.retention.exception.RetentionApiError.RETENTION_POLICY_TYPE_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionServiceImpl implements RetentionService {
    private final CaseRetentionRepository caseRetentionRepository;
    private final RetentionMapper retentionMapper;
    private final RetentionPolicyMapper retentionPolicyMapper;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Override
    public List<GetCaseRetentionsResponse> getCaseRetentions(Integer caseId) {
        List<CaseRetentionEntity> caseRetentionEntities =
            caseRetentionRepository.findByCaseId(caseId);

        List<GetCaseRetentionsResponse> caseRetentions = new ArrayList<>();
        for (CaseRetentionEntity caseRetentionEntity : caseRetentionEntities) {
            caseRetentions.add(retentionMapper.mapToCaseRetention(caseRetentionEntity));
        }
        return caseRetentions;
    }

    @Override
    public List<GetRetentionPolicy> getRetentionPolicyTypes() {
        final List<RetentionPolicyTypeEntity> policyTypeRepositoryAll = retentionPolicyTypeRepository.findAll();
        return retentionPolicyMapper.mapToRetentionPolicyResponse(policyTypeRepositoryAll);

    }

    @Override
    public GetRetentionPolicy getRetentionPolicyType(Integer id) {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = retentionPolicyTypeRepository
            .findById(id).orElseThrow(() -> new DartsApiException(RETENTION_POLICY_TYPE_ID_NOT_FOUND)
            );

        return retentionPolicyMapper.mapRetentionPolicy(retentionPolicyTypeEntity);
    }
}
