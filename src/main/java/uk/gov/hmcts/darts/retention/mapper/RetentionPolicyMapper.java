package uk.gov.hmcts.darts.retention.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.retentions.model.GetRetentionPolicy;

import java.util.List;


@Component
@RequiredArgsConstructor
public class RetentionPolicyMapper {
    public List<GetRetentionPolicy> mapToRetentionPolicyResponse(List<RetentionPolicyTypeEntity> retentionPolicyTypeEntity) {

        return retentionPolicyTypeEntity.stream().map(this::mapRetentionPolicy).toList();
    }

    public GetRetentionPolicy mapRetentionPolicy(RetentionPolicyTypeEntity entity) {
        GetRetentionPolicy getRetentionPolicy = new GetRetentionPolicy();
        getRetentionPolicy.setPolicyEndAt(entity.getPolicyEnd());
        getRetentionPolicy.setPolicyStartAt(entity.getPolicyStart());
        getRetentionPolicy.setId(entity.getId());
        getRetentionPolicy.setDisplayName(entity.getDisplayName());
        getRetentionPolicy.setName(entity.getPolicyName());
        getRetentionPolicy.setDescription(entity.getDescription());
        getRetentionPolicy.setFixedPolicyKey(entity.getFixedPolicyKey());
        getRetentionPolicy.setDuration(entity.getDuration());

        return getRetentionPolicy;
    }
}
