package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.mapper.RetentionPolicyTypeMapper;
import uk.gov.hmcts.darts.retention.service.RetentionPolicyTypeService;
import uk.gov.hmcts.darts.retention.validation.CreatePolicyTypeValidator;
import uk.gov.hmcts.darts.retention.validation.EditPolicyTypeValidator;
import uk.gov.hmcts.darts.retention.validation.LivePolicyValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyDisplayNameIsUniqueValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyDurationValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyHasNoPendingRevisionValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyNameIsUniqueValidator;
import uk.gov.hmcts.darts.retention.validation.PolicyStartDateIsFutureValidator;
import uk.gov.hmcts.darts.retention.validation.RevisePolicyTypeValidator;
import uk.gov.hmcts.darts.retentions.model.AdminPatchRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.AdminPostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.RetentionPolicyType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.CREATE_RETENTION_POLICY;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REVISE_RETENTION_POLICY;
import static uk.gov.hmcts.darts.retention.auditing.RetentionPolicyUpdateAuditActivityProvider.auditActivitiesFor;
import static uk.gov.hmcts.darts.retention.exception.RetentionApiError.RETENTION_POLICY_TYPE_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionPolicyTypeServiceImpl implements RetentionPolicyTypeService {

    private final RetentionPolicyTypeMapper retentionPolicyTypeMapper;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final AuthorisationApi authorisationApi;

    private final PolicyDurationValidator policyDurationValidator;
    private final CreatePolicyTypeValidator createPolicyTypeValidator;
    private final EditPolicyTypeValidator editPolicyTypeValidator;
    private final RevisePolicyTypeValidator revisePolicyTypeValidator;
    private final PolicyNameIsUniqueValidator policyNameIsUniqueValidator;
    private final PolicyDisplayNameIsUniqueValidator policyDisplayNameIsUniqueValidator;
    private final PolicyStartDateIsFutureValidator policyStartDateIsFutureValidator;
    private final LivePolicyValidator livePolicyValidator;
    private final PolicyHasNoPendingRevisionValidator policyHasNoPendingRevisionValidator;
    private final AuditApi auditApi;

    @Override
    public List<RetentionPolicyType> getRetentionPolicyTypes() {
        List<RetentionPolicyTypeEntity> policyTypeRepositoryAll = retentionPolicyTypeRepository.findAll(
            Sort.by(RetentionPolicyTypeEntity_.FIXED_POLICY_KEY).descending());

        ///admin/retention-policy-types
        return retentionPolicyTypeMapper.mapToModelList(policyTypeRepositoryAll);

    }

    @Override
    public RetentionPolicyType getRetentionPolicyType(Integer id) {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = retentionPolicyTypeRepository.findById(id)
            .orElseThrow(() -> new DartsApiException(RETENTION_POLICY_TYPE_ID_NOT_FOUND));

        return retentionPolicyTypeMapper.mapToModel(retentionPolicyTypeEntity);
    }

    @Override
    @Transactional
    public RetentionPolicyType createOrReviseRetentionPolicyType(AdminPostRetentionRequest adminPostRetentionRequest, Boolean isRevision) {
        policyDurationValidator.validate(adminPostRetentionRequest.getDuration());

        var fixedPolicyKey = adminPostRetentionRequest.getFixedPolicyKey();
        policyNameIsUniqueValidator.validate(adminPostRetentionRequest.getName(), fixedPolicyKey);
        policyDisplayNameIsUniqueValidator.validate(adminPostRetentionRequest.getDisplayName(), fixedPolicyKey);

        policyStartDateIsFutureValidator.validate(adminPostRetentionRequest.getPolicyStartAt());

        if (isRevision) {
            revisePolicyTypeValidator.validate(fixedPolicyKey);

            var priorPolicyEntity = retentionPolicyTypeRepository.findFirstByFixedPolicyKeyOrderByPolicyStartDesc(fixedPolicyKey)
                .orElseThrow();
            priorPolicyEntity.setPolicyEnd(adminPostRetentionRequest.getPolicyStartAt());
            retentionPolicyTypeRepository.saveAndFlush(priorPolicyEntity);

            auditApi.record(REVISE_RETENTION_POLICY);
        } else {
            createPolicyTypeValidator.validate(fixedPolicyKey);
        }

        var newPolicyEntity = retentionPolicyTypeMapper.mapToEntity(adminPostRetentionRequest);
        newPolicyEntity.setPolicyEnd(null); // Already implicit, but made explicit to make clear that new policies should always have an open-ended end date.

        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        newPolicyEntity.setCreatedBy(currentUser);
        newPolicyEntity.setLastModifiedBy(currentUser);

        RetentionPolicyTypeEntity createdEntity = retentionPolicyTypeRepository.save(newPolicyEntity);

        auditApi.record(CREATE_RETENTION_POLICY);

        return retentionPolicyTypeMapper.mapToModel(createdEntity);
    }

    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    public RetentionPolicyType editRetentionPolicyType(Integer id, AdminPatchRetentionRequest adminPatchRetentionRequest) {
        RetentionPolicyTypeEntity targetEntity = retentionPolicyTypeRepository.findById(id)
            .orElseThrow(() -> new DartsApiException(RETENTION_POLICY_TYPE_ID_NOT_FOUND));

        livePolicyValidator.validate(targetEntity.getPolicyStart());

        final var auditableActivities = auditActivitiesFor(targetEntity, adminPatchRetentionRequest);

        final String suppliedFixedPolicyKey = adminPatchRetentionRequest.getFixedPolicyKey();
        final String originalFixedPolicyKey = targetEntity.getFixedPolicyKey();
        if (suppliedFixedPolicyKey != null && !suppliedFixedPolicyKey.equals(originalFixedPolicyKey)) {
            handleFixedPolicyKeyChange(adminPatchRetentionRequest, originalFixedPolicyKey, suppliedFixedPolicyKey, targetEntity);
        }

        final String effectiveFixedPolicyKey = suppliedFixedPolicyKey == null ? originalFixedPolicyKey : suppliedFixedPolicyKey;
        final OffsetDateTime suppliedPolicyStartAt = adminPatchRetentionRequest.getPolicyStartAt();
        if (suppliedPolicyStartAt != null && !suppliedPolicyStartAt.isEqual(targetEntity.getPolicyStart())) {
            handlePolicyStartAtChange(effectiveFixedPolicyKey, suppliedPolicyStartAt, targetEntity);
        }

        String suppliedName = adminPatchRetentionRequest.getName();
        if (suppliedName != null) {
            policyNameIsUniqueValidator.validate(suppliedName, effectiveFixedPolicyKey);
            targetEntity.setPolicyName(suppliedName);
        }

        String suppliedDisplayName = adminPatchRetentionRequest.getDisplayName();
        if (suppliedDisplayName != null) {
            policyDisplayNameIsUniqueValidator.validate(suppliedDisplayName, effectiveFixedPolicyKey);
            targetEntity.setDisplayName(suppliedDisplayName);
        }

        String suppliedDescription = adminPatchRetentionRequest.getDescription();
        if (suppliedDescription != null) {
            targetEntity.setDescription(suppliedDescription);
        }

        String suppliedDuration = adminPatchRetentionRequest.getDuration();
        if (suppliedDuration != null) {
            policyDurationValidator.validate(suppliedDuration);
            targetEntity.setDuration(suppliedDuration);
        }

        RetentionPolicyTypeEntity updatedEntity = retentionPolicyTypeRepository.saveAndFlush(targetEntity);

        auditApi.recordAll(auditableActivities);

        return retentionPolicyTypeMapper.mapToModel(updatedEntity);
    }

    private void handleFixedPolicyKeyChange(AdminPatchRetentionRequest adminPatchRetentionRequest,
                                            String originalFixedPolicyKey,
                                            String suppliedFixedPolicyKey,
                                            RetentionPolicyTypeEntity targetEntity) {
        policyHasNoPendingRevisionValidator.validate(suppliedFixedPolicyKey);
        editPolicyTypeValidator.validate(suppliedFixedPolicyKey);
        targetEntity.setFixedPolicyKey(suppliedFixedPolicyKey);
        retentionPolicyTypeRepository.saveAndFlush(targetEntity);

        // Address impact to preceding original policy type
        findPriorPolicyForOriginalKey(originalFixedPolicyKey)
            .ifPresent(priorOriginalPolicy -> {
                priorOriginalPolicy.setPolicyEnd(null);
                retentionPolicyTypeRepository.save(priorOriginalPolicy);
            });

        // Address impact to target policy type
        findPriorPolicyForTargetKey(suppliedFixedPolicyKey)
            .ifPresent(priorNewPolicy -> {
                String effectiveName = adminPatchRetentionRequest.getName() == null
                    ? targetEntity.getPolicyName() : adminPatchRetentionRequest.getName();
                policyNameIsUniqueValidator.validate(effectiveName, suppliedFixedPolicyKey);

                String effectiveDisplayName = adminPatchRetentionRequest.getDisplayName() == null
                    ? targetEntity.getDisplayName() : adminPatchRetentionRequest.getDisplayName();
                policyDisplayNameIsUniqueValidator.validate(effectiveDisplayName, suppliedFixedPolicyKey);

                priorNewPolicy.setPolicyEnd(targetEntity.getPolicyStart());
                retentionPolicyTypeRepository.save(priorNewPolicy);
            });
    }

    private void handlePolicyStartAtChange(String effectiveFixedPolicyKey, OffsetDateTime suppliedPolicyStartAt, RetentionPolicyTypeEntity targetEntity) {
        policyStartDateIsFutureValidator.validate(suppliedPolicyStartAt);
        targetEntity.setPolicyStart(suppliedPolicyStartAt);

        findPriorPolicyForTargetKey(effectiveFixedPolicyKey)
            .ifPresent(priorPolicyEntity -> {
                priorPolicyEntity.setPolicyEnd(suppliedPolicyStartAt);
                retentionPolicyTypeRepository.save(priorPolicyEntity);
            });
    }

    private Optional<RetentionPolicyTypeEntity> findPriorPolicyForOriginalKey(String originalKey) {
        List<RetentionPolicyTypeEntity> entitiesByFixedPolicyKey = retentionPolicyTypeRepository.findByFixedPolicyKeyOrderByPolicyStartDesc(originalKey);
        if (!entitiesByFixedPolicyKey.isEmpty()) {
            return Optional.of(entitiesByFixedPolicyKey.get(0));
        }
        return Optional.empty();
    }

    private Optional<RetentionPolicyTypeEntity> findPriorPolicyForTargetKey(String targetKey) {
        List<RetentionPolicyTypeEntity> entitiesByFixedPolicyKey = retentionPolicyTypeRepository.findByFixedPolicyKeyOrderByPolicyStartDesc(targetKey);
        if (entitiesByFixedPolicyKey.size() > 1) {
            return Optional.of(entitiesByFixedPolicyKey.get(1));
        }
        return Optional.empty();
    }

}
