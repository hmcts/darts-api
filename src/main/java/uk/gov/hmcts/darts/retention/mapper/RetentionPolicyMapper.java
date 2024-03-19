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

        return retentionPolicyTypeEntity.stream().map(o -> {
            GetRetentionPolicy getRetentionPolicy = new GetRetentionPolicy();
            getRetentionPolicy.setPolicyEndAt(o.getPolicyEnd());
            getRetentionPolicy.setPolicyStartAt(o.getPolicyStart());
            getRetentionPolicy.setId(o.getId());
            getRetentionPolicy.setDisplayName(o.getDisplayName());
            getRetentionPolicy.setName(o.getPolicyName());
            getRetentionPolicy.setDescription(o.getDescription());
            getRetentionPolicy.setFixedPolicyKey(o.getFixedPolicyKey());
            getRetentionPolicy.setDuration(o.getDuration());

            return getRetentionPolicy;
        }).toList();
    }
}
