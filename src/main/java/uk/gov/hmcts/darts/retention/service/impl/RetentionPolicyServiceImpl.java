package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.mapper.RetentionPolicyTypeMapper;
import uk.gov.hmcts.darts.retention.service.RetentionPolicyService;
import uk.gov.hmcts.darts.retentions.model.AdminPostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.RetentionPolicy;

import java.util.List;

import static uk.gov.hmcts.darts.retention.exception.RetentionApiError.RETENTION_POLICY_TYPE_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionPolicyServiceImpl implements RetentionPolicyService {

    private final RetentionPolicyTypeMapper retentionPolicyTypeMapper;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final AuthorisationApi authorisationApi;
    private final Validator<AdminPostRetentionRequest> createOrRevisePolicyTypeValidator;
    private final Validator<String> policyDurationValidator;
    private final Validator<String> createPolicyTypeValidator;
    private final Validator<String> revisePolicyTypeValidator;

    @Override
    public List<RetentionPolicy> getRetentionPolicyTypes() {
        List<RetentionPolicyTypeEntity> policyTypeRepositoryAll = retentionPolicyTypeRepository.findAll();

        return retentionPolicyTypeMapper.mapToModelList(policyTypeRepositoryAll);

    }

    @Override
    public RetentionPolicy getRetentionPolicyType(Integer id) {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = retentionPolicyTypeRepository.findById(id)
            .orElseThrow(() -> new DartsApiException(RETENTION_POLICY_TYPE_ID_NOT_FOUND));

        return retentionPolicyTypeMapper.mapToModel(retentionPolicyTypeEntity);
    }

    @Override
    @Transactional
    public RetentionPolicy createOrReviseRetentionPolicyType(AdminPostRetentionRequest adminPostRetentionRequest, Boolean isRevision) {
        policyDurationValidator.validate(adminPostRetentionRequest.getDuration());
        createOrRevisePolicyTypeValidator.validate(adminPostRetentionRequest);

        var fixedPolicyKey = adminPostRetentionRequest.getFixedPolicyKey();
        if (isRevision) {
            revisePolicyTypeValidator.validate(fixedPolicyKey);

            var priorPolicyEntity = retentionPolicyTypeRepository.findFirstByFixedPolicyKeyOrderByPolicyStartDesc(fixedPolicyKey)
                .orElseThrow();
            priorPolicyEntity.setPolicyEnd(adminPostRetentionRequest.getPolicyStartAt());
            retentionPolicyTypeRepository.saveAndFlush(priorPolicyEntity);
        } else {
            createPolicyTypeValidator.validate(fixedPolicyKey);
        }

        var newPolicyEntity = retentionPolicyTypeMapper.mapToEntity(adminPostRetentionRequest);
        newPolicyEntity.setPolicyEnd(null); // Already implicit, but made explicit to make clear that new policies should always have an open-ended end date.

        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        newPolicyEntity.setCreatedBy(currentUser);
        newPolicyEntity.setLastModifiedBy(currentUser);

        RetentionPolicyTypeEntity createdEntity = retentionPolicyTypeRepository.save(newPolicyEntity);

        return retentionPolicyTypeMapper.mapToModel(createdEntity);
    }

}
